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
package fr.gouv.vitam.common.client;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import fr.gouv.vitam.common.exception.VitamClientInternalException;

/**
 * Vitam Restassured like client for Junit test</br>
 * </br>
 * Example:
 *
 * <pre>
 * <code>
 *   GET http://host:port/service/v1/resource/path1/monid1/path2/monid2
 *      Header: X-Request-ID = abcd
 *      Body = Json(body)
 *      Expected: OK
 *   int statusCode = testClient.given().accept(MediaType.APPLICATION_JSON_TYPE)
 *         .addHeader("X-Request-Id", "abcd")
 *         .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
 *         .body(body, MediaType.APPLICATION_JSON_TYPE)
 *         .status(Status.OK).get("resource");
 *
 *   POST http://host:port/service/v1/resource/path1/monid1/path2/monid2
 *      Header: X-Request-ID = abcd
 *      Body = Json(body)
 *      Expected: OK + Body: InputStream
 *   InputStream stream = testClient.given().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
 *         .addHeader("X-Request-Id", "abcd")
 *         .addPathParameter("path1", "monid1").addPathParameter("path2", "monid2")
 *         .body(body, MediaType.APPLICATION_JSON_TYPE)
 *         .status(Status.OK).get("resource", InputStream.class);
 * </code>
 * </pre>
 */
public class VitamRestTestClient extends DefaultClient {
    /**
     * Constructor using given scheme (http) and allowing multipart but no chunk
     *
     * @param factory The client factory
     */
    public VitamRestTestClient(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    /**
     *
     * @return a VitamRestTest using this client
     */
    public VitamRestTest given() {
        return new VitamRestTest(this);
    }

    /**
     * VItam Rest Test: mimic of Restassured.RequestSpecification
     */
    public static class VitamRestTest {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        FormDataMultiPart multiPart;
        Object body;
        MediaType contentType;
        List<String> pathParameters = new ArrayList<>();
        Status expectedStatus;
        MediaType acceptMediaType = MediaType.APPLICATION_JSON_TYPE;

        final VitamRestTestClient client;

        VitamRestTest(VitamRestTestClient client) {
            this.client = client;
        }

        @Override
        public String toString() {
            return new StringBuilder("VitamRestTest: { ")
                .append("Headers: { ").append(headers).append(" } ")
                .append(", Multipart: ").append(multiPart != null)
                .append(", Body: { value: ").append(body != null).append(", type: ").append(contentType).append(" } ")
                .append(", pathParameters: \"").append(pathParameters).append("\"")
                .append(", expectedStatus: ").append(expectedStatus)
                .append(", acceptedMediaType: ").append(acceptMediaType)
                .append(", ").append(client.toString())
                .append(" }").toString();
        }

        private void reset() {
            multiPart = null;
            body = null;
            contentType = null;
            headers.clear();
            pathParameters.clear();
            expectedStatus = null;
            acceptMediaType = MediaType.APPLICATION_JSON_TYPE;
        }

        /**
         *
         * @return this
         */
        public VitamRestTest then() {
            return this;
        }

        /**
         *
         * @return this
         */
        public VitamRestTest when() {
            return this;
        }

        /**
         *
         * @param status the expected status
         * @return this
         */
        public VitamRestTest status(Status status) {
            expectedStatus = status;
            return this;
        }

        /**
         *
         * @param statusCode the expected status
         * @return this
         */
        public VitamRestTest statusCode(int statusCode) {
            expectedStatus = Status.fromStatusCode(statusCode);
            return this;
        }

        /**
         *
         * @param accept the accept MediaType
         * @return this
         */
        public VitamRestTest accept(MediaType accept) {
            acceptMediaType = accept;
            return this;
        }

        /**
         * Note: if multipart is called, body is ignored
         *
         * @param controlName
         * @param object
         * @param mediaType
         * @return this
         */
        public VitamRestTest multiPart(String controlName, Object object, MediaType mediaType) {
            if (multiPart == null) {
                multiPart = new FormDataMultiPart();
            }
            multiPart.field(controlName, object, mediaType);
            return this;
        }

        /**
         * Note: if multipart is called, body is ignored
         *
         * @param controlName
         * @param fileName
         * @param stream
         * @return this
         */
        public VitamRestTest multiPart(String controlName, String fileName, InputStream stream) {
            if (multiPart == null) {
                multiPart = new FormDataMultiPart();
            }
            multiPart.bodyPart(
                new StreamDataBodyPart(controlName, stream, fileName, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            return this;
        }

        /**
         * Note: if multipart is called, body is ignored
         *
         * @param body
         * @param mediaType
         * @return this
         */
        public VitamRestTest body(Object body, MediaType mediaType) {
            this.body = body;
            contentType = mediaType;
            return this;
        }

        /**
         *
         * @param key
         * @param value
         * @return this
         */
        public VitamRestTest addHeader(String key, String value) {
            headers.add(key, value);
            return this;
        }

        /**
         * Note: add this method in order in addition to "path".</br>
         * Will add to final path + /name/{value}
         *
         * @param name
         * @param value
         * @return this
         */
        public VitamRestTest addPathParameter(String name, String value) {
            pathParameters.add("/" + name + "/" + value);
            return this;
        }

        private void checkStatus(int status) throws VitamClientInternalException {
            if (expectedStatus != null && status != expectedStatus.getStatusCode()) {
                throw new VitamClientInternalException(String.format("Status %d (%s) is not the one expected %d (%s)",
                    status, Status.fromStatusCode(status).getReasonPhrase(),
                    expectedStatus.getStatusCode(), expectedStatus.getReasonPhrase()));
            }
        }

        private String getFinalPath(String path) {
            final StringBuilder finalPath = new StringBuilder(path);
            for (final String subpath : pathParameters) {
                finalPath.append(subpath);
            }
            return finalPath.toString();
        }

        /**
         * The response content is ignored, only the status code is returned.
         *
         * @param httpMethod
         * @param path
         * @return the status code
         * @throws VitamClientInternalException if the status code is not the one expected (if specified)
         */
        public int execute(String httpMethod, String path) throws VitamClientInternalException {
            Response response = null;
            try {
                if (multiPart != null) {
                    body = multiPart;
                    contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
                }
                final String finalPath = getFinalPath(path);
                response = client.performRequest(httpMethod, finalPath, headers, body, contentType, acceptMediaType);
                final int status = response.getStatus();
                checkStatus(status);
                reset();
                return status;
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        }

        /**
         *
         * @param path
         * @return the status code
         * @throws VitamClientInternalException
         */
        public int get(String path) throws VitamClientInternalException {
            return execute(HttpMethod.GET, path);
        }

        /**
         *
         * @param path
         * @return the status code
         * @throws VitamClientInternalException
         */
        public int delete(String path) throws VitamClientInternalException {
            return execute(HttpMethod.DELETE, path);
        }

        /**
         *
         * @param path
         * @return the status code
         * @throws VitamClientInternalException
         */
        public int head(String path) throws VitamClientInternalException {
            return execute(HttpMethod.HEAD, path);
        }

        /**
         *
         * @param path
         * @return the status code
         * @throws VitamClientInternalException
         */
        public int options(String path) throws VitamClientInternalException {
            return execute(HttpMethod.OPTIONS, path);
        }

        /**
         *
         * @param path
         * @return the status code
         * @throws VitamClientInternalException
         */
        public int post(String path) throws VitamClientInternalException {
            return execute(HttpMethod.POST, path);
        }

        /**
         *
         * @param path
         * @return the status code
         * @throws VitamClientInternalException
         */
        public int put(String path) throws VitamClientInternalException {
            return execute(HttpMethod.PUT, path);
        }

        /**
         * The response content is returned according to type, except if status code is different than the one expected.
         * </br>
         * <b>Important:</b> if the entityType is an InputStream, it will be already consumed and closed.
         *
         * @param <T> the type of the entityType
         *
         * @param httpMethod
         * @param path
         * @param entityTpe
         * @return the entity of type <T>
         * @throws VitamClientInternalException if the status code is not the one expected (if specified)
         */
        public <T> T execute(String httpMethod, String path, Class<T> entityTpe)
            throws VitamClientInternalException {
            Response response = null;
            try {
                if (multiPart != null) {
                    body = multiPart;
                    contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
                }
                final String finalPath = getFinalPath(path);
                response = client.performRequest(httpMethod, finalPath, headers, body, contentType, acceptMediaType);
                final int status = response.getStatus();
                checkStatus(status);
                reset();
                return response.readEntity(entityTpe);
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        }

        /**
         *
         * @param path
         * @param entityTpe
         * @return the entity of type <T>
         * @throws VitamClientInternalException
         */
        public <T> T get(String path, Class<T> entityTpe) throws VitamClientInternalException {
            return execute(HttpMethod.GET, path, entityTpe);
        }

        /**
         *
         * @param path
         * @param entityTpe
         * @return the entity of type <T>
         * @throws VitamClientInternalException
         */
        public <T> T delete(String path, Class<T> entityTpe) throws VitamClientInternalException {
            return execute(HttpMethod.DELETE, path, entityTpe);
        }

        /**
         *
         * @param path
         * @param entityTpe
         * @return the entity of type <T>
         * @throws VitamClientInternalException
         */
        public <T> T options(String path, Class<T> entityTpe) throws VitamClientInternalException {
            return execute(HttpMethod.OPTIONS, path, entityTpe);
        }

        /**
         *
         * @param path
         * @param entityTpe
         * @return the entity of type <T>
         * @throws VitamClientInternalException
         */
        public <T> T post(String path, Class<T> entityTpe) throws VitamClientInternalException {
            return execute(HttpMethod.POST, path, entityTpe);
        }

        /**
         *
         * @param path
         * @param entityTpe
         * @return the entity of type <T>
         * @throws VitamClientInternalException
         */
        public <T> T put(String path, Class<T> entityTpe) throws VitamClientInternalException {
            return execute(HttpMethod.PUT, path, entityTpe);
        }
    }
}
