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
package fr.gouv.vitam.storage.offers.workspace.rest;


import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.core.WorkspaceConfiguration;

/**
 * DefaultOfferResource Test
 */
public class DefaultOfferResourceTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String WORKSPACE_OFFER_CONF = "default-offer.conf";
    private static File newWorkspaceOfferConf;

    private static final String REST_URI = "/offer/v1";
    private static int serverPort = 8784;
    private static JunitHelper junitHelper;
    private static final String OBJECTS_URI = "/objects";
    private static final String OBJECT_ID_URI = "/{id}";
    private static final String STATUS_URI = "/status";

    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";

    private static final ObjectMapper OBJECT_MAPPER;
    private static DefaultOfferApplication application;

    static {

        OBJECT_MAPPER = new ObjectMapper(new JsonFactory());
        OBJECT_MAPPER.disable(SerializationFeature.INDENT_OUTPUT);
    }

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferResourceTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        final File workspaceOffer = PropertiesUtils.findFile(WORKSPACE_OFFER_CONF);
        final DefaultOfferConfiguration realWorkspaceOffer =
            PropertiesUtils.readYaml(workspaceOffer, DefaultOfferConfiguration.class);
        newWorkspaceOfferConf = File.createTempFile("test", WORKSPACE_OFFER_CONF, workspaceOffer.getParentFile());
        PropertiesUtils.writeYaml(newWorkspaceOfferConf, realWorkspaceOffer);

        try {
            application = new DefaultOfferApplication(WORKSPACE_OFFER_CONF);
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Wokspace Offer Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        // junitHelper.releasePort(serverPort);
    }

    @After
    public void deleteExistingFiles() throws Exception {
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        File container = new File(conf.getStoragePath() + "/1" + this);
        File folder = new File(container.getAbsolutePath(), "/" + DataCategory.OBJECT.getFolder());
        final File object = new File(folder.getAbsolutePath(), "id1");
        final File object2 = new File(container.getAbsolutePath(), "id1");
        Files.deleteIfExists(object.toPath());
        Files.deleteIfExists(object2.toPath());
        Files.deleteIfExists(folder.toPath());
        Files.deleteIfExists(container.toPath());
        container = new File(conf.getStoragePath() + "/0" + this);
        folder = new File(container.getAbsolutePath(), "/" + DataCategory.OBJECT.getFolder());
        Files.deleteIfExists(folder.toPath());
        Files.deleteIfExists(container.toPath());
    }

    @Test
    public void getCapacityTestBadRequest() {
        given().when().get(OBJECTS_URI).then().statusCode(400);
    }

    @Test
    public void getCapacityTestOk() {
        // create tenant
        final ObjectInit objectInit = new ObjectInit();
        objectInit.setType(DataCategory.OBJECT);
        given().header(GlobalDataRest.X_TENANT_ID, "0" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectInit).when().post(OBJECTS_URI + "/" + "id1").then().statusCode(201);
        // test
        given().header(GlobalDataRest.X_TENANT_ID, "0" + this).when().get(OBJECTS_URI).then().statusCode(200);
    }

    @Test
    public void getObjectTestPreconditionFailed() {
        // no tenant id
        given().get(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(412);

    }

    @Test
    public void getObjectTestNotFound() {
        // not found
        given().header(GlobalDataRest.X_TENANT_ID, "1").contentType(MediaType.APPLICATION_JSON).then()
            .statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get(OBJECTS_URI + OBJECT_ID_URI, "id1");
    }


    @Test
    public void getObjectTestOK() throws Exception {

        final ObjectInit objectInit = new ObjectInit();
        objectInit.setType(DataCategory.OBJECT);
        with().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectInit).when().post(OBJECTS_URI + "/" + "id1");

        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            with().header(GlobalDataRest.X_TENANT_ID, "1" + this)
                .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_END)
                .contentType(MediaType.APPLICATION_OCTET_STREAM).content(in).when()
                .put(OBJECTS_URI + OBJECT_ID_URI, "id1");
        }

        // found
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).contentType(MediaType.APPLICATION_JSON).then()
            .statusCode(Status.OK.getStatusCode()).when()
            .get(OBJECTS_URI + OBJECT_ID_URI, DataCategory.OBJECT.getFolder() + "/id1");
    }

    @Test
    public void getObjectChunkTestOK() throws Exception {

        final ObjectInit objectInit = new ObjectInit();
        objectInit.setType(DataCategory.OBJECT);
        with().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectInit).when().post(OBJECTS_URI + "/" + "id1");

        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            final FileChannel fc = in.getChannel();
            final ByteBuffer bb = ByteBuffer.allocate(1024);

            byte[] bytes;
            int read = fc.read(bb);
            while (read >= 0) {
                bb.flip();
                if (fc.position() == fc.size()) {
                    bytes = new byte[read];
                    bb.get(bytes, 0, read);
                    try (InputStream inChunk = new ByteArrayInputStream(bytes)) {
                        assertNotNull(inChunk);
                        with().header(GlobalDataRest.X_TENANT_ID, "1" + this).header(GlobalDataRest.X_COMMAND,
                            StorageConstants.COMMAND_END)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM).content(inChunk).when()
                            .put(OBJECTS_URI + OBJECT_ID_URI, "id1");
                    }
                } else {
                    bytes = bb.array();
                    try (InputStream inChunk = new ByteArrayInputStream(bytes)) {
                        // assertNotNull(inChunk);
                        with().header(GlobalDataRest.X_TENANT_ID, "1" + this)
                            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_WRITE)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM).content(inChunk).when()
                            .put(OBJECTS_URI + OBJECT_ID_URI, "id1");
                    }
                }
                bb.clear();
                read = fc.read(bb);
            }
        }

        // found
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).contentType(MediaType.APPLICATION_JSON).then()
            .statusCode(Status.OK.getStatusCode()).when()
            .get(OBJECTS_URI + OBJECT_ID_URI, DataCategory.OBJECT.getFolder() + "/id1");
    }

    @Test
    public void postObjectsTest() throws Exception {
        final String guid = GUIDFactory.newGUID().toString();
        // no tenant id
        given().contentType(MediaType.APPLICATION_JSON).when().post(OBJECTS_URI + "/" + guid).then().statusCode(400);

        // no command
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).contentType(MediaType.APPLICATION_JSON).when()
            .post(OBJECTS_URI + "/" +
                guid)
            .then().statusCode(400);

        // no ObjectInit, command != INIT
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_END)
            .contentType(MediaType.APPLICATION_JSON).when()
            .post(OBJECTS_URI + "/" + guid).then().statusCode(400);

        // no ObjectInit
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON).when().post(OBJECTS_URI + "/" + guid).then().statusCode(400);

        final ObjectInit objectInit = new ObjectInit();
        objectInit.setType(DataCategory.OBJECT);
        assertNotNull(objectInit);

        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectInit).when().post(OBJECTS_URI + "/" + guid).then().statusCode(201);

        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + "/1" + this);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());
        final File folder = new File(container.getAbsolutePath() + "/" + DataCategory.OBJECT.getFolder());
        assertTrue(folder.exists());
        assertTrue(folder.isDirectory());
    }


    @Test
    public void putObjectTest() throws Exception {
        // no tenant id
        given().contentType(MediaType.APPLICATION_OCTET_STREAM).when().put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
            .statusCode(400);

        // No command
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).contentType(MediaType.APPLICATION_OCTET_STREAM).when()
            .put(OBJECTS_URI +
                OBJECT_ID_URI, "id1")
            .then().statusCode(400);

        // Bad command
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_OCTET_STREAM).when().put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
            .statusCode(400);

        // No INIT
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            // try only with one chunk
            final byte[] bytes = new byte[1024];
            in.read(bytes);
            try (InputStream inChunk = new ByteArrayInputStream(bytes)) {
                assertNotNull(inChunk);
                given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
                    .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_WRITE)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).content(inChunk).when()
                    .put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
                    .statusCode(500);
            }
        }

        final ObjectInit objectInit = new ObjectInit();
        objectInit.setType(DataCategory.OBJECT);
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectInit).when().post(OBJECTS_URI + "/" + "id1").then().statusCode(201);
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
                .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_END)
                .contentType(MediaType.APPLICATION_OCTET_STREAM).content(in).when()
                .put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
                .statusCode(201);
        }
        // check
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + "/1" + this);
        assertNotNull(container);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());
        final File folder = new File(container.getAbsolutePath(), "/" + DataCategory.OBJECT.getFolder());
        assertNotNull(folder);
        assertTrue(folder.exists());
        assertTrue(folder.isDirectory());
        final File object = new File(folder.getAbsolutePath(), "id1");
        assertNotNull(object);
        assertTrue(object.exists());
        assertFalse(object.isDirectory());

        assertTrue(com.google.common.io.Files.equal(PropertiesUtils.findFile(ARCHIVE_FILE_TXT), object));
    }

    // TODO activate when chunk mode is done in {@see DefaultOfferService} method createObject
    @Test
    @Ignore
    public void putObjectChunkTest() throws Exception {
        // no tenant id
        given().contentType(MediaType.APPLICATION_OCTET_STREAM).when().put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
            .statusCode(400);

        // No command
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).contentType(MediaType.APPLICATION_OCTET_STREAM).when()
            .put(OBJECTS_URI +
                OBJECT_ID_URI, "id1")
            .then().statusCode(400);

        // Bad command
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_OCTET_STREAM).when().put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
            .statusCode(400);

        // No INIT
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            // try only with one chunk
            final byte[] bytes = new byte[1024];
            in.read(bytes);
            try (InputStream inChunk = new ByteArrayInputStream(bytes)) {
                assertNotNull(inChunk);
                given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
                    .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_WRITE)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).content(inChunk).when()
                    .put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
                    .statusCode(500);
            }
        }

        final ObjectInit objectInit = new ObjectInit();
        objectInit.setType(DataCategory.OBJECT);
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_INIT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectInit).when().post(OBJECTS_URI + "/" + "id1").then().statusCode(201);
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            final FileChannel fc = in.getChannel();
            final ByteBuffer bb = ByteBuffer.allocate(1024);

            byte[] bytes;
            int read = fc.read(bb);
            while (read >= 0) {
                bb.flip();
                if (fc.position() == fc.size()) {
                    bytes = new byte[read];
                    bb.get(bytes, 0, read);
                    try (InputStream inChunk = new ByteArrayInputStream(bytes)) {
                        assertNotNull(inChunk);
                        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).header(GlobalDataRest.X_COMMAND,
                            StorageConstants.COMMAND_END)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM).content(inChunk).when()
                            .put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
                            .statusCode(201);
                    }
                } else {
                    bytes = bb.array();
                    try (InputStream inChunk = new ByteArrayInputStream(bytes)) {
                        assertNotNull(inChunk);
                        given().header(GlobalDataRest.X_TENANT_ID, "1" + this)
                            .header(GlobalDataRest.X_COMMAND, StorageConstants.COMMAND_WRITE)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM).content(inChunk).when()
                            .put(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
                            .statusCode(201);
                    }
                }
                bb.clear();
                read = fc.read(bb);
            }
        }
        // check
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + "/1" + this);
        assertNotNull(container);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());
        final File folder = new File(container.getAbsolutePath(), "/" + DataCategory.OBJECT.getFolder());
        assertNotNull(folder);
        assertTrue(folder.exists());
        assertTrue(folder.isDirectory());
        final File object = new File(folder.getAbsolutePath(), "id1");
        assertNotNull(object);
        assertTrue(object.exists());
        assertFalse(object.isDirectory());

        assertTrue(com.google.common.io.Files.equal(PropertiesUtils.findFile(ARCHIVE_FILE_TXT), object));
    }

    @Test
    public void headObjectTest() throws Exception {
        // no tenant id
        given().head(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(400);

        // no object
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).head(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
            .statusCode(404);

        // object
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + "/1" + this);
        container.mkdir();
        final File object = new File(container.getAbsolutePath(), "id1");
        object.createNewFile();
        given().header(GlobalDataRest.X_TENANT_ID, "1" + this).head(OBJECTS_URI + OBJECT_ID_URI, "id1").then()
            .statusCode(204);
    }

    @Test
    public void deleteObjectTest() {
        given().delete(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(501);
    }

    @Test
    public void statusTest() {
        given().get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

}
