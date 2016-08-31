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
package fr.gouv.vitam.processing.worker.handler;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.StorageInformation;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;

/**
 * CheckStorageAvailability Handler.<br>
 */
public class CheckStorageAvailabilityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(CheckStorageAvailabilityActionHandler.class);

    private static final String HANDLER_ID = "CheckStorageAvailability";

    private final SedaUtilsFactory sedaUtilsFactory;
    private static final StorageClient STORAGE_CLIENT = StorageClientFactory.getInstance().getStorageClient();
    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     * @param factory the seda utils factory
     */
    public CheckStorageAvailabilityActionHandler(SedaUtilsFactory factory) {
        sedaUtilsFactory = factory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public EngineResponse execute(WorkParams params) {
        ParametersChecker.checkParameter("params is a mandatory parameter", params);
        ParametersChecker.checkParameter("ServerConfiguration is a mandatory parameter",
            params.getServerConfiguration());
        LOGGER.info("CheckStorageAvailabilityActionHandler running ...");

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK);

        final SedaUtils sedaUtils = sedaUtilsFactory.create();
        long totalSizeToBeStored;
        String messageId = "";
        try {            
            messageId = sedaUtils.getMessageIdentifier(params);
            long objectsSizeInSip = sedaUtils.computeTotalSizeOfObjectsInManifest(params);
            long manifestSize = sedaUtils.getManifestSize(params);
            totalSizeToBeStored = objectsSizeInSip + manifestSize;

            JsonNode storageCapacityNode = STORAGE_CLIENT.getStorageInformation(DEFAULT_TENANT, DEFAULT_STRATEGY);
            // TODO : add JsonNode to POJO functionality in JsonHandler. For the moment we have to convert to
            // JsonNode to a string then convert it back to a POJO.
            StorageInformation information =
                JsonHandler.getFromString(JsonHandler.writeAsString(storageCapacityNode), StorageInformation.class);
            long storageCapacity = information.getUsableSpace();
            if (storageCapacity >= totalSizeToBeStored) {
                response.setStatus(StatusCode.OK);
            } else {
                // error - KO
                response.setStatus(StatusCode.KO).setOutcomeMessages(HANDLER_ID, OutcomeMessage.STORAGE_OFFER_SPACE_KO);
            }
        } catch (ProcessingException | StorageNotFoundClientException | StorageServerClientException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO).setOutcomeMessages(HANDLER_ID, OutcomeMessage.STORAGE_OFFER_KO_UNAVAILABLE);
        }
        response.setMessageIdentifier(messageId);
        return response;
    }
}