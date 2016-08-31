/**
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
 */
package fr.gouv.vitam.core;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.server.application.configuration.DbConfiguration;
import fr.gouv.vitam.core.database.collections.MetadataCollections;
import fr.gouv.vitam.core.database.collections.MongoDbAccessMetadataImpl;
/**
 * Factory to get MongoDbAccess for Metadata
 */
public class MongoDbAccessMetadataFactory {
    
    /**
     * Creation of one MongoDbAccess
     *
     * @param configuration config of MongoDbAcess
     * @return the MongoDbAccess
     * @throws IllegalArgumentException if argument is null
     */
    public MongoDbAccessMetadataImpl create(MetaDataConfiguration configuration) {
        ParametersChecker.checkParameter("configuration is a mandatory parameter", configuration);
        List<Class<?>> classList = new ArrayList<>();
        for (MetadataCollections e : MetadataCollections.class.getEnumConstants()) {
            classList.add(e.getClasz());
        }
        MetadataCollections.class.getEnumConstants();
        return new MongoDbAccessMetadataImpl(
            new MongoClient(new ServerAddress(
                configuration.getHost(),
                configuration.getPort()),
                VitamCollection.getMongoClientOptions(classList)),
            configuration.getDbName(), true);
    }
}