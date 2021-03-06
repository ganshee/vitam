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
package fr.gouv.vitam.access.external.client;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Access External Client Interface
 */
public interface AccessExternalClient extends BasicClient {

    /**
     * selectUnits /units
     *
     * @param selectQuery
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    RequestResponse selectUnits(JsonNode selectQuery)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

    /**
     * selectUnitbyId GET(POST overrided) /units/{id}
     *
     * @param selectQuery
     * @param unitId
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    RequestResponse selectUnitbyId(JsonNode selectQuery, String unitId)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

    /**
     * updateUnitbyId UPDATE /units/{id}
     *
     * @param updateQuery
     * @param unitId
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    RequestResponse updateUnitbyId(JsonNode updateQuery, String unitId)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

    /**
     * getObjectAsInputStream
     *
     * @param selectQuery
     * @param objectId
     * @param usage
     * @param version
     * @return Response including InputStream
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    Response getObject(JsonNode selectQuery, String objectId, String usage, int version)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

    /**
     * selectObjectById
     *
     * @param selectQuery
     * @param unitId
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    RequestResponse selectObjectById(JsonNode selectQuery, String unitId)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

    /**
     * selectOperation
     *
     * @param select
     * @return Json representation
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse selectOperation(JsonNode select) throws LogbookClientException, InvalidParseOperationException;

    /**
     * selectOperationbyId
     *
     * @param processId
     * @return Json representation
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse selectOperationbyId(String processId) throws LogbookClientException, InvalidParseOperationException;

    /**
     * selectUnitLifeCycleById
     *
     * @param idUnit
     * @return Json representation
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse selectUnitLifeCycleById(String idUnit)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * selectObjectGroupLifeCycleById
     *
     * @param idObject
     * @return Json representation
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse selectObjectGroupLifeCycleById(String idObject)
        throws LogbookClientException, InvalidParseOperationException;



    /**
     * Get the accession register summary matching the given query
     *
     * @param query The DSL Query as Json Node
     * @return The AccessionregisterSummary list as a response JsonNode
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    RequestResponse getAccessionRegisterSummary(JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

    /**
     * Get the accession register details matching the given query
     *
     * @param id
     * @param query The DSL Query as a JSON Node
     * @return The AccessionregisterDetails list as a response jsonNode
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     */
    RequestResponse getAccessionRegisterDetail(String id, JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException;

}


