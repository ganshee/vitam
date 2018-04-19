/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.functional.administration.contract.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.openstack4j.api.exceptions.StatusCode;

import javax.ws.rs.core.Response;


public class IngestContractImplTest {


    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;
    private static final Integer EXTERNAL_TENANT = 2;
    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "IngestContract";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    static VitamCounterService vitamCounterService;
    static MetaDataClient metaDataClientMock;
    static FunctionalBackupService functionalBackupService;

    static ContractService<IngestContractModel> ingestContractService;
    static int mongoPort;
    private static MongoDbAccessAdminImpl dbImpl;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        client = new MongoClient(new ServerAddress(DATABASE_HOST, mongoPort));

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));
        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        final List tenants = new ArrayList<>();
        tenants.add(new Integer(TENANT_ID));
        tenants.add(new Integer(EXTERNAL_TENANT));
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("INGEST_CONTRACT");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);


        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);
        LogbookOperationsClientFactory.changeMode(null);

        metaDataClientMock = mock(MetaDataClient.class);

        functionalBackupService = mock(FunctionalBackupService.class);

        ingestContractService =
            new IngestContractImpl(dbImpl, vitamCounterService, metaDataClientMock, functionalBackupService);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        ingestContractService.close();
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
        Mockito.reset(functionalBackupService);
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk());
        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("IC-000");
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("IC-000");

        verify(functionalBackupService).saveCollectionAndSequence(any(), eq(IngestContractImpl.CONTRACT_BACKUP_EVENT),
            eq(FunctionalAdminCollections.INGEST_CONTRACT), any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestMissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_missingName.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());

        verifyNoMoreInteractions(functionalBackupService);

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestProfileNotInDBReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_profile_not_indb.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());
    }

    @Test
    @RunWithCustomExecutor
    public void testObjectNode() throws InvalidParseOperationException {
        final ArrayNode object = JsonHandler.createArrayNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("Status", "update");
        msg.put("oldStatus", "INACTIF");
        msg.put("newStatus", "ACTIF");
        final ObjectNode msg2 = JsonHandler.createObjectNode();
        msg2.put("LinkParentId", "update");
        msg2.put("oldLinkParentId", "lqskdfjh");
        msg2.put("newLinkParentId", "lqskdfjh");
        object.add(msg);
        object.add(msg2);
        final String wellFormedJson = SanityChecker.sanitizeJson(object);
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestProfileInDBReturnOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");

        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {
            });

        dbImpl.insertDocuments(
            JsonHandler.createArrayNode().add(JsonHandler.toJsonNode(profileModelList.iterator().next())),
            FunctionalAdminCollections.PROFILE).close();

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_profile_indb.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk());
        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(1);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // Try to recreate the same contract but with id
        response = ingestContractService.createContracts(responseCast.getResults());

        assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotUniqueName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        // unset ids
        IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find ingestContract with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * String q = "{ \"$query\" : [ { \"$eq\" : { \"_id\" : \"fake_id\" } } ] }"; JsonNode queryDsl =
         * JsonHandler.getFromString(q);
         */
        final RequestResponseOK<IngestContractModel> IngestContractModelList =
            ingestContractService.findContracts(queryDsl);

        assertThat(IngestContractModelList.getResults()).isEmpty();
    }


    /**
     * Check that the created ingest conrtact have the tenant owner after persisted to database
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(Integer.valueOf(TENANT_ID));

    }


    /**
     * Ingest contract of tenant 1, try to get the same contract with id mongo but with tenant 2 This sgould not return
     * the contract as tenant 2 is not the owner of the Ingest contract
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();


        VitamThreadUtils.getVitamSession().setTenantId(2);

        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNull();

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestfindByIdentifier() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestImportExternalIdentifier() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok_identifier.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);
        assertThat(response.isOk()).isTrue();

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        // test duplication
        response = ingestContractService.createContracts(IngestContractModelList);
        assertThat(response.isOk()).isFalse();
    }



    @Test
    @RunWithCustomExecutor
    public void givenIngesContractsTestImportExternalIdentifierKO() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);
        assertThat(response.isOk()).isFalse();

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<IngestContractModel> IngestContractModelList =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindAllThenReturnTwoContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<IngestContractModel> IngestContractModelListSearch =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> ingestModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(ingestModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = ingestContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
        }
        final String identifier = responseCast.getResults().get(0).getIdentifier();
        // Test update for ingest contract Status => inactive
        final String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive = UpdateActionHelper.set("Status", ActivationStatus.INACTIVE.toString());
        final SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", identifier));
        update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateContractStatus =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate);
        assertThat(updateContractStatus).isNotExactlyInstanceOf(VitamError.class);

        parser.parse(new Select().getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", identifier));
        final JsonNode queryDsl2 = parser.getRequest().getFinalSelect();
        final RequestResponseOK<IngestContractModel> ingestContractModelListForassert =
            ingestContractService.findContracts(queryDsl2);
        assertThat(ingestContractModelListForassert.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : ingestContractModelListForassert.getResults()) {
            assertThat(ActivationStatus.INACTIVE.equals(ingestContractModel.getStatus())).isTrue();
            assertThat(ingestContractModel.getDeactivationdate()).isNotEmpty();
            assertThat(ingestContractModel.getLastupdate()).isNotEmpty();
        }

        // Test update for ingest contract Status => Active
        final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusActive = UpdateActionHelper.set("Status", ActivationStatus.ACTIVE.toString());
        final SetAction setActionDesactivationDateActive = UpdateActionHelper.set("ActivationDate", now);
        final SetAction setActionLastUpdateActive = UpdateActionHelper.set("LastUpdate", now);
        final SetAction setLinkParentId = UpdateActionHelper.set(IngestContractModel.LINK_PARENT_ID, "");
        final Update updateStatusActive = new Update();
        updateStatusActive.setQuery(QueryHelper.eq("Identifier", identifier));
        updateStatusActive.addActions(setActionStatusActive, setActionDesactivationDateActive,
            setActionLastUpdateActive, setLinkParentId);
        updateParserActive.parse(updateStatusActive.getFinalUpdate());
        JsonNode queryDslStatusActive = updateParserActive.getRequest().getFinalUpdate();
        ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslStatusActive);
        
        final RequestResponseOK<IngestContractModel> ingestContractModelListForassert2 =
            ingestContractService.findContracts(queryDsl2);
        assertThat(ingestContractModelListForassert2.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : ingestContractModelListForassert2.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
            assertThat(ingestContractModel.getActivationdate()).isNotEmpty();
            assertThat(ingestContractModel.getLastupdate()).isNotEmpty();
            assertThat(ingestContractModel.getLinkParentId()).isEqualTo("");
            assertThat(ActivationStatus.INACTIVE.equals(ingestContractModel.getCheckParentLink()));
        }
        
        // we try to update ingest contract with same value -> Bad Request
        RequestResponse responseUpdate =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslStatusActive);
        assertThat(!responseUpdate.isOk());
        assertEquals(responseUpdate.getStatus(), StatusCode.BAD_REQUEST.getCode());
        
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final IngestContractModel acm = IngestContractModelList.iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        final String name = acm.getName();
        assertThat(name).isNotNull();


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", name));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();


        final RequestResponseOK<IngestContractModel> IngestContractModelListFound =
            ingestContractService.findContracts(queryDsl);
        assertThat(IngestContractModelListFound.getResults()).hasSize(1);

        final IngestContractModel acmFound = IngestContractModelListFound.getResults().iterator().next();
        assertThat(acmFound).isNotNull();

        assertThat(acmFound.getId()).isEqualTo(id1);
        assertThat(acmFound.getName()).isEqualTo(name);

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestLinkParentIdKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(metaDataClientMock.selectUnitbyId(anyObject(), anyObject()))
            .thenReturn(new RequestResponseOK<>().toJsonNode());

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_link_parentId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
        });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(response).isInstanceOf(VitamError.class);

        final VitamError vitamError = (VitamError) response;
        assertThat(vitamError.toString()).contains("is not in filing nor holding schema");
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestLinkParentIdOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponseOK ok = new RequestResponseOK<>();
        ok.setHits(1, 0, 1, 1);// simulate returning result when query for filing or holding unit
        when(metaDataClientMock.selectUnitbyId(anyObject(), anyObject())).thenReturn(ok.toJsonNode());

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_link_parentId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
        });
        final RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<IngestContractModel> IngestContractModelListSearch =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestValidationWhenUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> ingestModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        final RequestResponse response = ingestContractService.createContracts(ingestModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = ingestContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
        }
        final String identifier = responseCast.getResults().get(0).getIdentifier();
        // Test update for ingest contract Status => inactive
        final String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive = UpdateActionHelper.set("Status", "INVALID_STATUS");
        final SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", identifier));
        update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateContractStatus =
                ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate);
        assertTrue(updateContractStatus.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateContractStatus).isInstanceOf(VitamError.class);
        List<VitamError> errors = ((VitamError) updateContractStatus).getErrors();
        assertThat(VitamLogbookMessages.getFromFullCodeKey(errors.get(0).getMessage()).equals(
            "Échec du processus de mise à jour du contrat d'entrée : une valeur ne correspond pas aux valeurs attendues")
        ).isTrue();
        
        final UpdateParserSingle updateParser2 = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setCheckParentLinkStatusInactive = UpdateActionHelper.set("CheckParentLink", "INVALID_STATUS");
        final Update update2 = new Update();
        update2.setQuery(QueryHelper.eq("Identifier", identifier));
        update2.addActions(setCheckParentLinkStatusInactive);
        updateParser2.parse(update2.getFinalUpdate());
        JsonNode queryDslForUpdate2 = updateParser2.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateCheckParentLinkStatus =
                ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate2);
        assertTrue(updateCheckParentLinkStatus.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateCheckParentLinkStatus).isInstanceOf(VitamError.class);
    }

}
