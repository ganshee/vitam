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
package fr.gouv.vitam.ingest.external.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import fr.gouv.vitam.ingest.external.api.IngestExternalException;

public class IngestExternalClientMockTest {

    private static final String MOCK_INPUT_STREAM = "VITAM-Ingest External Client Mock InputStream";

    @Test(expected = IngestExternalException.class)
    public void givenNullStreamThenThrowIngestExternalException() throws IngestExternalException, XMLStreamException {
        IngestExternalClientFactory.changeMode(null);

        final IngestExternalClient client =
            IngestExternalClientFactory.getInstance().getClient();
        assertTrue(client instanceof IngestExternalClientMock);
        client.upload(null);
    }


    @Test
    public void givenNonEmptyStreamThenUploadWithSuccess() throws IngestExternalException, XMLStreamException {
        IngestExternalClientFactory.changeMode(null);

        final IngestExternalClient client =
            IngestExternalClientFactory.getInstance().getClient();
        assertNotNull(client);

        final InputStream firstStream = IOUtils.toInputStream(MOCK_INPUT_STREAM);
        final InputStream responseStream = client.upload(firstStream).readEntity(InputStream.class);

        final InputStream inputstreamMockATR =
            IOUtils.toInputStream(IngestExternalClientMock.MOCK_INGEST_EXTERNAL_RESPONSE_STREAM);
        assertNotNull(responseStream);
        try {
            assertTrue(IOUtils.contentEquals(responseStream, inputstreamMockATR));
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
