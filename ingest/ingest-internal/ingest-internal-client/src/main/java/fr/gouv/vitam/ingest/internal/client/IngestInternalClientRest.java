/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.ingest.internal.client;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.internal.model.UploadResponseDTO;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;


/**
 * Rest client implementation for Ingest Internal
 */
public class IngestInternalClientRest implements IngestInternalClient{

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalClientRest.class);
    private static final String RESOURCE_PATH = "/ingest/v1";
    private static final String UPLOAD_URL = "/upload";
    private static final String STATUS_URL = "/status";

    private final String serviceUrl;
    private final Client client;

    IngestInternalClientRest(String server, int port) {
    	 ParametersChecker.checkParameter("server and port are a mandatory parameter",server, port);
        serviceUrl = "http://" + server + ":" + port + RESOURCE_PATH;
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        config.register(MultiPartFeature.class);
        client = ClientBuilder.newClient(config);
    }

    
    @Override
    public int status() {
    	
    	return client.target(serviceUrl).path(STATUS_URL).request().get().getStatus();
    }
    
    @Override
    public UploadResponseDTO upload(List<LogbookParameters> logbookParametersList, InputStream inputStream) throws VitamException{

    	ParametersChecker.checkParameter("check Upload Parameter", logbookParametersList );
    	final FormDataMultiPart multiPart = new FormDataMultiPart();
    	
    	multiPart.field("part", logbookParametersList, MediaType.APPLICATION_JSON_TYPE);
    	
    	if(inputStream!=null){
    	multiPart.bodyPart(
    			new StreamDataBodyPart("part", inputStream, "SIP", MediaType.APPLICATION_OCTET_STREAM_TYPE));
    	}
    	
    	final Response response = client.target(serviceUrl).path(UPLOAD_URL).request()
    			.post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));

    	if (Status.OK.getStatusCode() == response.getStatus()) {
    		LOGGER.info("SIP : " + Response.Status.OK.getReasonPhrase());
    	} else {
    		LOGGER.error("SIP Upload Error");
    		throw new VitamException("SIP Upload");
    	}

    	return response.readEntity(UploadResponseDTO.class);
    }

}