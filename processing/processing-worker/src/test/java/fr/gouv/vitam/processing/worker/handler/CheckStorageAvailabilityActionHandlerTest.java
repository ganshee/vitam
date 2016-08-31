package fr.gouv.vitam.processing.worker.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;

public class CheckStorageAvailabilityActionHandlerTest {

    CheckStorageAvailabilityActionHandler handler;
    private static final String HANDLER_ID = "CheckStorageAvailability";
    private SedaUtilsFactory factory;
    private SedaUtils sedaUtils;

    @Before
    public void setUp() {
        factory = mock(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
    }

    @Test
    public void givenSedaNotExistWhenCheckStorageThenReturnResponseFatal() throws Exception {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils)
            .computeTotalSizeOfObjectsInManifest(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new CheckStorageAvailabilityActionHandler(factory);
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""))
                .setGuuid("objectGroupId")
                .setObjectName("objectGroupId.json").setContainerName("containerName").setCurrentStep("INGEST");
        final EngineResponse response = handler.execute(params);
        assertEquals(StatusCode.KO, response.getStatus());
        assertTrue(response.getOutcomeMessages().values().contains(OutcomeMessage.STORAGE_OFFER_KO_UNAVAILABLE));
    }

    @Test
    public void givenSedaExistWhenCheckStorageExecuteThenReturnResponseKO() throws Exception {
        Mockito.doReturn(new Long(838860800)).when(sedaUtils)
            .computeTotalSizeOfObjectsInManifest(anyObject());
        Mockito.doReturn(new Long(838860800)).when(sedaUtils)
            .getManifestSize(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new CheckStorageAvailabilityActionHandler(factory);
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""))
                .setGuuid("objectGroupId")
                .setObjectName("objectGroupId.json").setContainerName("containerName").setCurrentStep("INGEST");
        final EngineResponse response = handler.execute(params);
        assertEquals(StatusCode.KO, response.getStatus());
        assertTrue(response.getOutcomeMessages().values().contains(OutcomeMessage.STORAGE_OFFER_SPACE_KO));
    }
    

    @Test
    public void givenSedaExistWhenCheckStorageExecuteThenReturnResponseOK() throws Exception {
        Mockito.doReturn(new Long(1024)).when(sedaUtils)
            .computeTotalSizeOfObjectsInManifest(anyObject());
        Mockito.doReturn(new Long(1024)).when(sedaUtils)
            .getManifestSize(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new CheckStorageAvailabilityActionHandler(factory);
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""))
                .setGuuid("objectGroupId")
                .setObjectName("objectGroupId.json").setContainerName("containerName").setCurrentStep("INGEST");
        final EngineResponse response = handler.execute(params);
        assertEquals(StatusCode.OK, response.getStatus());
    }

}