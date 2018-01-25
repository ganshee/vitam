package fr.gouv.vitam.worker.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.model.LogbookLifeCycleTraceabilityHelper;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, LogbookLifeCyclesClientFactory.class,
	LogbookOperationsClientFactory.class})
public class LogbookLifeCycleTraceabilityHelperTest {
	private static final String FILE_NAME = "0_operations_20171031_151118.zip";
	private static final String LOGBOOK_OPERATION_START_DATE = "2017-08-17T14:01:07.52";
	private static final String LOGBOOK_OPERATION_END_DATE = "2017-08-16T14:34:40.426";
	private static final String LAST_OPERATION_HASH =
	    "MIIEljAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEewYJKoZIhvcNAQcCoIIEbDCCBGgCAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARA25rjfWLQKTkp0ETrnTQ1b/iZ8fIhx8lsguGt2wv8UuYjtbD5PYZ/LydCEVWBSi5HwJ8E1PZbRIvsH+R2sGhy4QIBARgPMjAxNzA4MTcxNTAxMDZaMYIDzTCCA8kCAQEwfjB4MQswCQYDVQQGEwJmcjEMMAoGA1UECAwDaWRmMQ4wDAYDVQQHDAVwYXJpczEOMAwGA1UECgwFdml0YW0xFDASBgNVBAsMC2F1dGhvcml0aWVzMSUwIwYDVQQDDBxjYV9pbnRlcm1lZGlhdGVfdGltZXN0YW1waW5nAgIAujANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzA4MTcxNTAxMDZaMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQLja866EXeQzwV2WyARNL+C3Gh9jbJQDtmlAtLxbgFjNZkkPHGjY83b0imbLbpCeU7kr3jrvo+dLIOJgSh/IfXMwZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQCURZjpTzSgWrppLklHIw5xgA8HXuv0mqAnhOCqmsyuuiWcWjCT3H42RDJSWaTCtFP/xa6tgHOynRG+4X5CHKmQwDQYJKoZIhvcNAQENBQAEggIANO7owkRFd4iTLs+RmM0cNrGvy6LhrkaV2r6862E3G5jBmC2Ao8WkI0chahPy+2gHi90M2ykpwTicoRbYBL4s9XlZn2KJ0fA2HZ/f283nacB4ARO+tdQRs7p8vXgyPYC9kO59fa/he7B1o0Mdo6uwba053r7JJplx6hNnCiJM3bB5jTBoxpdb3A2o+cq/TdGqM4MVwYms1jbswF4UDzWBLnKwY4cw/vGCuelw2AU+Q5B11QxrHjXVHaeeVm6ju27YtkGOWthwF3KQ6LEe6xKka+XQZ8kwxHIh523WjrMpoH+B8BTNRerO6KnhxVfHKUKTDO/zpYhPXyKjibg2d3lkRCUa1jtFoBKIBsdvDz0cEoN2XuOkIm9tMpe5pE4gvPRVToTJe7YxZePrvlvmJfwM5RNuNvMqvWlq3CgPj77BePzZGCfSgG91/h0TCAwQXJDEyvk9PJOrjNt4ABNJ6YOxCRF/IeQyEtUpJ9yP13JXOTTRaKkDuueObjnemxvS68rs5h1elqgFdWDCfT9TJtpEmERIT9+8+uf/v8fpSkAFpHKIaZjoAQjIBvJYSvyGZlUCrEUoAtxVgVWGMOXZITc6UADOasv8Fjm10lg+2bgX/KP1S+hH68/lMH6RNKmsC1/BKEsH9+cnsdTJySPS2HjZmfZ5FK695FRQpjL5wfgKbK8=";
	private static final String HANDLER_ID = "FINALIZE_LC_TRACEABILITY";
	private static final String LAST_OPERATION = "FinalizeLifecycleTraceabilityActionHandler/lastOperation.json";
	private static final String TRACEABILITY_INFO =
			"FinalizeLifecycleTraceabilityActionHandler/traceabilityInformation.json";
    private static final String OBJECT_FILE =
            "FinalizeLifecycleTraceabilityActionHandler/object.txt";
        private static final String UNIT_FILE =
            "FinalizeLifecycleTraceabilityActionHandler/unit.txt";

	private static LocalDateTime LOGBOOK_OPERATION_EVENT_DATE;
	private HandlerIOImpl handlerIO;
	private List<IOParameter> in;
	private WorkspaceClient workspaceClient;
	private WorkspaceClientFactory workspaceClientFactory;
	private LogbookOperationsClientFactory logbookOperationsClientFactory;
	private LogbookOperationsClient logbookOperationsClient;
	private ItemStatus itemStatus;
    private final List<URI> uriListWorkspaceOKUnit = new ArrayList<>();
    private final List<URI> uriListWorkspaceOKObj = new ArrayList<>();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		LOGBOOK_OPERATION_EVENT_DATE = LocalDateUtil.fromDate(LocalDateUtil.getDate("2016-06-10T11:56:35.914"));

		PowerMockito.mockStatic(WorkspaceClientFactory.class);
		workspaceClient = mock(WorkspaceClient.class);
		workspaceClientFactory = mock(WorkspaceClientFactory.class);
		PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
		PowerMockito.when(WorkspaceClientFactory.getInstance().getClient())
		.thenReturn(workspaceClient);

		PowerMockito.mockStatic(LogbookOperationsClientFactory.class);
		logbookOperationsClient = mock(LogbookOperationsClient.class);
		logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
		PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookOperationsClientFactory);
		PowerMockito.when(LogbookOperationsClientFactory.getInstance().getClient()).thenReturn(logbookOperationsClient);

		handlerIO = new HandlerIOImpl(workspaceClient, "FinalizeLifecycleTraceabilityActionHandlerTest", "workerId");
		in = new ArrayList<>();
		in.add(new IOParameter()
		.setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/lastOperation.json")));
		in.add(new IOParameter()
		.setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/traceabilityInformation.json")));
        uriListWorkspaceOKUnit
            .add(new URI(URLEncoder.encode("aeaqaaaaaqhgausqab7boak55nw5vqaaaaaq.json", CharsetUtils.UTF_8)));
        uriListWorkspaceOKObj
            .add(new URI(URLEncoder.encode("aebaaaaaaahgausqab7boak55jchzyqaaaaq.json", CharsetUtils.UTF_8)));
		itemStatus = new ItemStatus(HANDLER_ID);
	}

	@Test
	@RunWithCustomExecutor
	public void should_extract_correctly_startDate_from_last_event() throws Exception {
		// Given
		GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
		handlerIO.addInIOParameters(in);
		
		LogbookLifeCycleTraceabilityHelper helper = 
				new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId());

		// When
		LocalDateTime startDate = helper.getLastEvent();

		// Then
		assertThat(startDate).isEqualTo(LOGBOOK_OPERATION_EVENT_DATE);
	}

	@Test
	@RunWithCustomExecutor
	public void should_extract_correctly_startDate_from_no_event() throws Exception {
		// Given
		GUID guid = GUIDFactory.newOperationLogbookGUID(0);
		handlerIO.addInIOParameters(in);

		LogbookLifeCycleTraceabilityHelper helper = 
				new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId());

		// When
		LocalDateTime startDate = helper.getLastEvent();

		// Then
		assertThat(startDate).isEqualTo(LogbookTraceabilityHelper.INITIAL_START_DATE);
	}

	@Test
	@RunWithCustomExecutor
	public void should_correctly_save_data_and_compute_start_date_for_first_traceability() throws Exception {
		// Given
		GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
		handlerIO.addInIOParameters(in);
		
		when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_OBJECTS_FOLDER)))
	    .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKObj));
	when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), eq(SedaConstants.LFC_UNITS_FOLDER)))
	    .thenReturn(new RequestResponseOK().addResult(uriListWorkspaceOKUnit));
	InputStream objectFile = new FileInputStream(PropertiesUtils.findFile(OBJECT_FILE));
    InputStream unitFile = new FileInputStream(PropertiesUtils.findFile(UNIT_FILE));
    when(workspaceClient.getObject(anyObject(),
        eq(SedaConstants.LFC_OBJECTS_FOLDER + "/aebaaaaaaahgausqab7boak55jchzyqaaaaq.json")))
            .thenReturn(Response.status(Status.OK).entity(objectFile).build());
    when(workspaceClient.getObject(anyObject(),
        eq(SedaConstants.LFC_UNITS_FOLDER + "/aeaqaaaaaqhgausqab7boak55nw5vqaaaaaq.json")))
            .thenReturn(Response.status(Status.OK).entity(unitFile).build());
    when(logbookOperationsClient.selectOperation(anyObject()))
        .thenThrow(new LogbookClientException("LogbookClientException"));

		LogbookLifeCycleTraceabilityHelper helper = 
				new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId());

		LocalDateTime initialStartDate = LogbookTraceabilityHelper.INITIAL_START_DATE;

		final MerkleTreeAlgo algo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

		File zipFile = new File(folder.newFolder(), String.format(FILE_NAME));
		TraceabilityFile file = new TraceabilityFile(zipFile, helper.getTraceabilityType().getFileName());

		// When
		helper.saveDataInZip(algo, initialStartDate, file);
		file.close();

		// Then
		assertThat(Files.size(Paths.get(zipFile.getPath()))).isEqualTo(31875);
	}

	@Test
	@RunWithCustomExecutor
	public void should_return_null_date_and_token_if_no_previous_logbook() throws Exception {
		// Given
		GUID guid = GUIDFactory.newOperationLogbookGUID(0);
		handlerIO.addInIOParameters(in);

		LogbookLifeCycleTraceabilityHelper helper = 
				new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId());

		helper.getLastEvent();
		
		// When
		String date = helper.getPreviousStartDate();
		byte[] token = helper.getPreviousTimestampToken();

		// Then
		assertThat(date).isNull();
		assertThat(token).isNull();
	}

	@Test
	@RunWithCustomExecutor
	public void should_extract_correctly_date_and_token_from_last_event() throws Exception {
		// Given
		GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
		handlerIO.addInIOParameters(in);

		LogbookLifeCycleTraceabilityHelper helper = 
				new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId());

		helper.getLastEvent();

		// When
		String date = helper.getPreviousStartDate();
		byte[] token = helper.getPreviousTimestampToken();

		// Then
		assertThat(date).isEqualTo(LOGBOOK_OPERATION_START_DATE);
		assertThat(Base64.encodeBase64String(token)).isEqualTo(LAST_OPERATION_HASH);
	}

	@Test
	@RunWithCustomExecutor
	public void should_extract_correctly_event_number_and_date() throws Exception {
		// Given
		GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
		handlerIO.addInIOParameters(in);

		LogbookLifeCycleTraceabilityHelper helper = 
				new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId());

		helper.getLastEvent();

		// When
		Long size = helper.getDataSize();
		String endDate = helper.getEndDate();

		// Then
		assertThat(size).isEqualTo(5);
		assertThat(endDate).isEqualTo(LOGBOOK_OPERATION_END_DATE);
	}
}