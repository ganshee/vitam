/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.worker.server.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.VitamServerFactory;

/**
 *
 */
public class WorkerApplicationTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerApplicationTest.class);
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String WORKER_CONF = "worker-test.conf";
    private static int serverPort;
    private static final String DATABASE_HOST = "localhost";
    private static int oldPort;
    private static int databasePort;
    private static JunitHelper junitHelper;
    private static File worker;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();


        databasePort = junitHelper.findAvailablePort();
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        worker = PropertiesUtils.findFile(WORKER_CONF);
        final WorkerConfiguration realWorker = PropertiesUtils.readYaml(worker, WorkerConfiguration.class);
        realWorker.setRegisterServerPort(serverPort).setRegisterServerHost("localhost")
            .setRegisterDelay(1).setRegisterRetry(1).setProcessingUrl("http://localhost:8888").setDbHost(DATABASE_HOST)
            .setDbPort(databasePort).setDbName("Vitam-test");

        try (FileOutputStream outputStream = new FileOutputStream(worker)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(outputStream, realWorker);
        }
        serverPort = junitHelper.findAvailablePort();
        // TODO verifier la compatibilité avec les tests parallèles sur jenkins
        JunitHelper.setJettyPortSystemProperty(serverPort);

        oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(serverPort);
        junitHelper.releasePort(databasePort);
        VitamServerFactory.setDefaultPort(oldPort);
    }

    @Test
    public final void testFictiveLaunch() {
        try {
            new WorkerApplication(WORKER_CONF);
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }
}
