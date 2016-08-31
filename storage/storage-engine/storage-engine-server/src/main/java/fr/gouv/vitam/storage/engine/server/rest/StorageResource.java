/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.storage.engine.server.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusMessage;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.RequestResponseError;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.model.response.VitamError;
import fr.gouv.vitam.storage.engine.server.distribution.DataCategory;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionImpl;

/**
 * Storage Resource implementation
 */
@Path("/storage/v1")
public class StorageResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResource.class);

    private final StorageDistribution distribution;

    /*
    * TODO : Use custom ObjectMapper because the ObjectMapper used in JsonHandler requires UPPER Camel case
    * json attribute to be able to map automatically json properties (ie: without field annotation) to java
    * attributes. Need a discussion on this with Frédéric.
    */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Constructor
     *
     * @param configuration the storage configuration to be applied
     */
    public StorageResource(StorageConfiguration configuration) {
        distribution = new StorageDistributionImpl(configuration);
        LOGGER.info("init Storage Resource server");
    }

    /**
     * Constructor used for test purpose
     *
     * @param storageDistribution the storage Distribution to be applied
     */
    StorageResource(StorageDistribution storageDistribution) {
        distribution = storageDistribution;
    }



    /**
     * Return a response status
     *
     * @return Response containing the status of the service
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.ok(new StatusMessage(ServerIdentity.getInstance()),
            MediaType.APPLICATION_JSON).build();
    }

    /**
     * @param headers http headers
     * @return null if strategy and tenant headers have values, an error response otherwise
     */
    private Response checkTenantStrategyHeader(HttpHeaders headers) {
        Status status;
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.STRATEGY_ID)) {
            status = Status.PRECONDITION_FAILED;
            return buildErrorResponse(status);
        }
        return null;
    }

    /**
     * Get storage information for a specific tenant/strategy
     * For example the usable space
     *
     * @param headers http headers
     * @return Response containing the storage information as json, or an error (404, 500)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStorageInformation(@Context HttpHeaders headers) {
        Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            Status status;
            String tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                JsonNode result = distribution.getContainerInformation(tenantId, strategyId);
                return Response.status(Status.OK).entity(result).build();
            } catch (StorageNotFoundException exc) {
                LOGGER.error(exc);
                status = Status.NOT_FOUND;
            } catch (StorageTechnicalException exc) {
                LOGGER.error(exc);
                status = Status.INTERNAL_SERVER_ERROR;
            }
            return buildErrorResponse(status);
        }
        return response;
    }

    /**
     * Search the header value for 'X-Http-Method-Override' and return an error response id it's value is not 'GET'
     *
     * @param headers the http headers to check
     * @return OK response if no header is found, NULL if header value is correct, BAD_REQUEST if the header contain
     * an other value than GET
     */
    public Response checkPostHeader(HttpHeaders headers) {
        if (HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
            MultivaluedHashMap<String, String> wanted = new MultivaluedHashMap<>();
            wanted.add(VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET);
            try {
                HttpHeaderHelper.validateHeaderValue(headers, wanted);
                return null;
            } catch (IllegalArgumentException | IllegalStateException exc) {
                LOGGER.error(exc);
                return badRequestResponse(exc.getMessage());
            }
        } else {
            return Response.status(Status.OK).build();
        }
    }


    /**
     * Get a list of containers
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http headers
     * @return Response containing the storage information as an input stream, or an error (412 or 404)
     * @throws IOException throws an IO Exception
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    // TODO si le résultat est une liste alors getContainers (s ajouté)
    public Response getContainer(@Context HttpHeaders headers) throws IOException {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Create a container
     * <p>
     * TODO : container creation possibility needs to be re-think then deleted or implemented. Vitam Architects
     * are aware of this
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a container
     *
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a container
     *
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }


    /**
     * Get a list of objects
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjects(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get object metadata as json
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectInformation(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get an object data
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     * @throws IOException throws an IO Exception
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    public Response getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId)
        throws IOException {
        Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            Status status;
            String tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                InputStream result = distribution.getContainerObject(tenantId, strategyId, objectId);
                return Response.status(Status.OK).entity(result).build();
            } catch (StorageNotFoundException exc) {
                LOGGER.error(exc);
                status = Status.NOT_FOUND;
            } catch (StorageTechnicalException exc) {
                LOGGER.error(exc);
                status = Status.INTERNAL_SERVER_ERROR;
            }
            // If here, an error occurred
            return buildErrorResponse(status);
        }
        return response;
    }


    /**
     * Post a new object
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    @Path("/objects/{id_object}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectOrGetInformation(@Context HttpHeaders headers, @PathParam("id_object") String objectId,
        CreateObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            Response response = checkTenantStrategyHeader(headers);
            if (response == null) {
                Status status;
                String tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
                String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
                try {
                    StoredInfoResult result = distribution.storeData(tenantId, strategyId, objectId,
                        createObjectDescription, DataCategory.OBJECT, null);
                    return Response.status(Status.CREATED).entity(result).build();
                } catch (StorageNotFoundException exc) {
                    LOGGER.error(exc);
                    status = Status.NOT_FOUND;
                } catch (StorageTechnicalException exc) {
                    LOGGER.error(exc);
                    status = Status.INTERNAL_SERVER_ERROR;
                }
                // If here, an error occurred
                return buildErrorResponse(status);
            }
            return response;
        } else {
            return getObjectInformationWithPost(headers, objectId);
        }
    }

    private Response getObjectInformationWithPost(HttpHeaders headers, String objectId) {
        Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getObjectInformation(headers, objectId);
        } else if (responsePost.getStatus() == Status.OK.getStatusCode()) {
            return Response.status(Status.PRECONDITION_FAILED).build();
        } else {
            return responsePost;
        }
    }

    /**
     * Retrieve an object data (equivalent to GET with body)
     * @param headers http headers
     * @param objectId the object identifier to retrieve
     * @return Precondtion failed or not yet implemented for now
     * @throws IOException in case of any i/o exception
     */
    @Path("/objects/{id_object}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM + "; qs=0.3", StorageConstants.APPLICATION_ZIP + "; qs=0.3"})
    public Response getObjectWithPost(@Context HttpHeaders headers, @PathParam("id_object") String objectId)
        throws IOException {
        Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getObject(headers, objectId);
        } else if (responsePost.getStatus() == Status.OK.getStatusCode()) {
            return Response.status(Status.PRECONDITION_FAILED).build();
        } else {
            return responsePost;
        }
    }

    /**
     * Delete an object
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        Status status;
        String tenantId, strategyId;
        Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                distribution.deleteObject(tenantId, strategyId, objectId);
                return Response.status(Status.NO_CONTENT).build();
            } catch (StorageNotFoundException e) {
                LOGGER.error(e);
                status = Status.NOT_FOUND;
                return buildErrorResponse(status);
            }
        }
        return response;
    }

    /**
     * Check the existence of an object
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        Status status;
        String tenantId, strategyId;
        Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                distribution.getContainerObjectInformations(tenantId, strategyId, objectId);
                return Response.status(Status.OK).build();
            } catch (StorageNotFoundException e) {
                LOGGER.error(e);
                status = Status.NOT_FOUND;
                return buildErrorResponse(status);
            }
        }
        return response;
    }


    /**
     * Get a list of logbooks
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogbooks(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get an object
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers   http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new object
     *
     * @param logbook   the logbook to be created
     * @param headers   http header
     * @param logbookId the id of the logbookId
     * @return Response NOT_IMPLEMENTED
     */
    // FIXME always sending position from Workspace, not directly data (json)
    @Path("/logbooks/{id_logbook}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLogbook(JsonNode logbook, @Context HttpHeaders headers,
        @PathParam("id_logbook") String logbookId) {
        Status status;
        Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getLogbook(headers, logbookId);
        } else if (responsePost.getStatus() == Status.OK.getStatusCode()) {
            Response response = checkTenantStrategyHeader(headers);
            if (response == null) {
                String tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
                String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
                try {
                    StoredInfoResult result =
                        distribution.storeData(tenantId, strategyId, logbookId, null, DataCategory.LOGBOOK, logbook);
                    return Response.status(Status.CREATED).entity(result).build();
                } catch (StorageNotFoundException e) {
                    LOGGER.error(e);
                    status = Status.NOT_FOUND;
                } catch (StorageTechnicalException e) {
                    LOGGER.error(e);
                    status = Status.INTERNAL_SERVER_ERROR;
                }
                // If here, an error occurred
                return buildErrorResponse(status);
            }
            return response;
        } else {
            return responsePost;
        }
    }

    /**
     * Delete a logbook
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers   http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a logbook
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers   http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }


    /**
     * Get a list of units
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnits(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get a unit
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    // FIXME always sending position from Workspace, not directly data (json)
    /**
     * Post a new unit metadata
     *
     * @param unit       the unit to be created
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUnitMetadata(JsonNode unit, @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId) {

        Status status;
        Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getUnit(headers, metadataId);
        } else if (responsePost.getStatus() == Status.OK.getStatusCode()) {
            Response response = checkTenantStrategyHeader(headers);
            if (response == null) {
                String tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
                String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
                try {
                    StoredInfoResult result = distribution.storeData(tenantId, strategyId, metadataId, null,
                        DataCategory.UNIT, unit);
                    return Response.status(Status.CREATED).entity(result).build();
                } catch (StorageNotFoundException e) {
                    LOGGER.error(e);
                    status = Status.NOT_FOUND;
                } catch (StorageTechnicalException e) {
                    LOGGER.error(e);
                    status = Status.INTERNAL_SERVER_ERROR;
                }
                return buildErrorResponse(status);
            }
            return response;
        } else {
            return responsePost;
        }
    }

    /**
     * Update a unit metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @param query the query as a JsonNode
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUnitMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId,
        JsonNode query) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a unit metadata
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a unit metadata
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get a list of Object Groups
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectGroups(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    // FIXME always sending position from Workspace, not directly data (json)
    /**
     * Get a Object Group
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new Object Group metadata
     *
     * Note : this is NOT to be handled in item #72.
     *
     * @param object     the object to be created
     * @param headers    http header
     * @param metadataId the id of the Object Group metadata
     * @return Response Created, not found or internal server error
     */
    // TODO : check the existence, in the headers, of the value X-Http-Method-Override, if set
    @Path("/objectgroups/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectGroup(JsonNode object, @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId) {
        Status status;
        Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getObjectGroup(headers, metadataId);
        } else if (responsePost.getStatus() == Status.OK.getStatusCode()) {
            Response response = checkTenantStrategyHeader(headers);
            if (response == null) {
                String tenantId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.TENANT_ID).get(0);
                String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
                try {
                    StoredInfoResult result = distribution.storeData(tenantId, strategyId, metadataId, null,
                        DataCategory.OBJECT_GROUP, object);
                    return Response.status(Status.CREATED).entity(result).build();
                } catch (StorageNotFoundException e) {
                    LOGGER.error(e);
                    status = Status.NOT_FOUND;
                } catch (StorageTechnicalException e) {
                    LOGGER.error(e);
                    status = Status.INTERNAL_SERVER_ERROR;
                }
                // If here, an error occurred
                return buildErrorResponse(status);
            }
            return response;
        } else {
            return responsePost;
        }
    }

    /**
     * Update a Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @param query the query as a JsonNode
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateObjectGroupMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId,
        JsonNode query) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a Object Group metadata
     *
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a Object Group metadata
     *
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the Object Group metadata
     * @return Response OK if the object exists, NOT_FOUND otherwise (or BAD_REQUEST in cas of bad request format)
     */
    @Path("/objectgroups/{id_md}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    private Response buildErrorResponse(Status status) {
        return Response.status(status).entity((new RequestResponseError().setError(
            new VitamError(status.getStatusCode())
                .setContext("storage")
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(status.getReasonPhrase()))).toString())
            .build();
    }

    private Response badRequestResponse(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"" + message + "\"}").build();
    }

}