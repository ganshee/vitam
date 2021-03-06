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
package fr.gouv.vitam.worker.core.handler;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Computes archive unit 's Management date
 */
public class UnitsRulesComputeHandler extends ActionHandler {


    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UnitsRulesComputeHandler.class);

    private static final String HANDLER_ID = "UNITS_RULES_COMPUTE";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String AU_PREFIX_WITH_END_DATE = "WithEndDte_";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final String AU_NOT_HAVE_RULES = "Archive unit does not have rules";
    private static final String CHECKS_RULES = "Rules checks problem: missing parameters";


    private HandlerIO handlerIO;

    private final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters = LogbookParametersFactory
        .newLogbookLifeCycleUnitParameters();

    /**
     * Empty constructor UnitsRulesComputeHandler
     *
     */
    public UnitsRulesComputeHandler() {
        // Empty
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("UNITS_RULES_COMPUTE in execute");
        final long time = System.currentTimeMillis();
        handlerIO = handler;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        final String objectID = LogbookLifecycleWorkerHelper.getObjectID(params);

        try {
            try {
                LogbookLifecycleWorkerHelper.updateLifeCycleStartStep(handlerIO.getHelper(),
                    logbookLifecycleUnitParameters,
                    params, HANDLER_ID, LogbookTypeProcess.INGEST);


                calculateMaturityDate(params, itemStatus);
                itemStatus.increment(StatusCode.OK);
            } catch (final ProcessingException e) {
                LOGGER.debug(e);
                itemStatus.increment(StatusCode.KO);
            }

            // Update lifeCycle
            try {
                logbookLifecycleUnitParameters.setFinalStatus(HANDLER_ID, null, itemStatus.getGlobalStatus(),
                    null);
                LogbookLifecycleWorkerHelper.setLifeCycleFinalEventStatusByStep(handlerIO.getHelper(),
                    logbookLifecycleUnitParameters,
                    itemStatus);

            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        } finally {
            try {
                handlerIO.getLifecyclesClient().bulkUpdateUnit(params.getContainerName(),
                    handlerIO.getHelper().removeUpdateDelegate(objectID));
            } catch (LogbookClientNotFoundException | LogbookClientBadRequestException |
                LogbookClientServerException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }
        LOGGER.debug("[exit] execute... /Elapsed Time:" + (System.currentTimeMillis() - time) / 1000 + "s");
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }


    private void calculateMaturityDate(WorkerParameters params, ItemStatus itemStatus) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        try (InputStream inputStream =
            handlerIO.getInputStreamFromWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName)) {
            // Parse RULES in management Archive unit, and add EndDate
            parseXmlRulesAndUpdateEndDate(inputStream, objectName, containerId, params, itemStatus);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException |
            XMLStreamException e) {
            LOGGER.error(WORKSPACE_SERVER_ERROR);
            throw new ProcessingException(e);
        }
    }

    /**
     * findRulesValueQueryBuilders: select query
     *
     * @param rulesId
     * @return the JsonNode answer
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     * @throws ProcessingException
     */

    private JsonNode findRulesValueQueryBuilders(Set<String> rulesId)
        throws InvalidCreateOperationException, InvalidParseOperationException,
        IOException, ProcessingException {
        final Select select =
            new Select();
        select.addOrderByDescFilter(FileRules.RULEID);
        final BooleanQuery query = or();
        for (final String ruleId : rulesId) {
            query.add(eq(FileRules.RULEID, ruleId));
        }
        select.setQuery(query);

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            return adminManagementClient.getRules(select.getFinalSelect());
        } catch (final VitamException e) {
            throw new ProcessingException(e);
        }

    }

    /**
     *
     * parses xml unit file and add endate
     *
     * @param xmlInput
     * @param params
     * @param itemStatus
     * @throws IOException
     * @throws XMLStreamException
     * @throws ProcessingException
     */
    private void parseXmlRulesAndUpdateEndDate(InputStream xmlInput, String objectName, String containerName,
        WorkerParameters params, ItemStatus itemStatus)
        throws IOException, XMLStreamException, ProcessingException {
        Set<String> rulesToApply;
        JsonNode rulesResults = null;
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        final File fileWithEndDate = handlerIO.getNewLocalFile(AU_PREFIX_WITH_END_DATE + objectName);
        final FileWriter tmpFileWriter = new FileWriter(fileWithEndDate);
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(tmpFileWriter);
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEventReader reader = null;
        XMLEvent event = null;
        try {
            reader = xmlInputFactory.createXMLEventReader(xmlInput);
            while (true) {
                event = reader.nextEvent();
                if (event.isStartElement()) {
                    switch (event.asStartElement().getName().getLocalPart()) {
                        case SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT:
                            writer.add(event);

                            event = (XMLEvent) reader.next();
                            writer.add(event);
                            // list of rules
                            if (event.isCharacters()) {
                                rulesToApply = Sets
                                    .newHashSet(Splitter.on(SedaConstants.RULE_SEPARATOR)
                                        .split(event.asCharacters().getData()));
                                if (rulesToApply == null || rulesToApply.isEmpty()) {
                                    LOGGER.debug(AU_NOT_HAVE_RULES);

                                    if (!fileWithEndDate.delete()) {
                                        LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                                    }
                                    return;
                                }

                                // search ref rules
                                rulesResults = findRulesValueQueryBuilders(rulesToApply);
                                LOGGER.debug("rulesResults for archive unit id: " + objectName +
                                    " && containerName is :" + containerName + " is :" + rulesResults);
                            }
                            // end of list
                            event = (XMLEvent) reader.next();
                            writer.add(event);
                            break;
                        case SedaConstants.TAG_RULE_ACCESS:
                        case SedaConstants.TAG_RULE_REUSE:
                        case SedaConstants.TAG_RULE_STORAGE:
                        case SedaConstants.TAG_RULE_APPRAISAL:
                        case SedaConstants.TAG_RULE_CLASSIFICATION:
                        case SedaConstants.TAG_RULE_DISSEMINATION: {
                            writer.add(event);
                            String ruleId = "";
                            String startDate = "";
                            String endDateAsString = "";
                            String currentRuleType = event.asStartElement().getName().getLocalPart();

                            boolean isNotEndRuleTag = true;
                            while (isNotEndRuleTag) {
                                event = reader.nextEvent();
                                if (event.isStartElement()) {
                                    switch (event.asStartElement().getName().getLocalPart()) {
                                        case SedaConstants.TAG_RULE_RULE:
                                            writer.add(event);
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            if (event.isCharacters()) {
                                                ruleId = event.asCharacters().getData();
                                            }
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            break;
                                        case SedaConstants.TAG_RULE_START_DATE:
                                            writer.add(event);
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            if (event.isCharacters()) {
                                                startDate = event.asCharacters().getData();
                                            }
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            // add End date
                                            endDateAsString =
                                                getEndDate(startDate, ruleId, rulesResults, currentRuleType);

                                            if (StringUtils.isNotBlank(endDateAsString)) {
                                                writer.add(eventFactory.createStartElement("", "",
                                                    SedaConstants.TAG_RULE_END_DATE));
                                                writer.add(eventFactory.createCharacters(endDateAsString));
                                                writer.add(eventFactory.createEndElement("", "",
                                                    SedaConstants.TAG_RULE_END_DATE));
                                            }
                                            break;

                                        default:
                                            writer.add(event);
                                    }


                                } else if (event.isEndElement()) {
                                    switch (event.asEndElement().getName()
                                        .getLocalPart()) {
                                        case SedaConstants.TAG_RULE_ACCESS:
                                        case SedaConstants.TAG_RULE_REUSE:
                                        case SedaConstants.TAG_RULE_STORAGE:
                                        case SedaConstants.TAG_RULE_APPRAISAL:
                                        case SedaConstants.TAG_RULE_CLASSIFICATION:
                                        case SedaConstants.TAG_RULE_DISSEMINATION: {
                                            isNotEndRuleTag = false;
                                            writer.add(event);
                                            break;
                                        }
                                        default:
                                            writer.add(event);
                                    }
                                } else {
                                    writer.add(event);
                                }
                            }
                            // break supported rule
                            break;
                        }

                        default:
                            writer.add(event);
                    }

                } else if (event.isEndElement()) {

                    if (IngestWorkflowConstants.ROOT_TAG.equals(event.asEndElement().getName().getLocalPart())) {
                        writer.add(event);
                        break;
                    }

                    writer.add(event);

                } else {
                    writer.add(event);
                }
            }

        } catch (final Exception e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (final XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }

        // Write to workspace
        try {
            handlerIO.transferFileToWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName,
                fileWithEndDate, true);
        } catch (final ProcessingException e) {
            LOGGER.error("Can not write to workspace ", e);
            if (!fileWithEndDate.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }
            throw e;
        }
    }

    private JsonNode getRuleNodeByID(String ruleId, String ruleType, JsonNode jsonResult) {
        if (jsonResult != null) {
            final ArrayNode rulesResult = (ArrayNode) jsonResult.get("$results");
            for (final JsonNode rule : rulesResult) {
                final String ruleIdFromList = rule.get(FileRules.RULEID).asText();
                final String ruleTypeFromList = rule.get(FileRules.RULETYPE).asText();
                if (!StringUtils.isBlank(ruleId) && !StringUtils.isBlank(ruleType) && ruleId.equals(ruleIdFromList) &&
                    ruleType.equals(ruleTypeFromList)) {
                    return rule;
                }
            }
        }
        return JsonHandler.createObjectNode();
    }

    private String getEndDate(String startDateString, String ruleId, JsonNode rulesResults, String currentRuleType)
        throws FileRulesException, InvalidParseOperationException, ParseException, ProcessingException {
        if (!StringUtils.isBlank(startDateString) && !StringUtils.isBlank(ruleId) &&
            !StringUtils.isBlank(currentRuleType)) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            final Date startDate = simpleDateFormat.parse(startDateString);
            final JsonNode ruleNode = getRuleNodeByID(ruleId, currentRuleType, rulesResults);
            if (checkRulesParameters(ruleNode)) {
                final String duration = ruleNode.get(FileRules.RULEDURATION).asText();
                final String measurement = ruleNode.get(FileRules.RULEMEASUREMENT).asText();
                final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromMonth(measurement);
                final int calendarUnit = ruleMeasurement.getCalendarUnitType();
                final Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(calendarUnit, Integer.parseInt(duration));
                return simpleDateFormat.format(cal.getTime());
            } else {
                throw new ProcessingException(CHECKS_RULES);
            }
        }
        return "";

    }

    /**
     * @param ruleNode
     */
    private boolean checkRulesParameters(JsonNode ruleNode) {
        return ruleNode != null && ruleNode.get(FileRules.RULEDURATION) != null &&
            ruleNode.get(FileRules.RULEMEASUREMENT) != null;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
}
