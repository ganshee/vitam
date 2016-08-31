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
package fr.gouv.vitam.logbook.common.parameters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class LogbookParametersSerializerTest {

    @Test
    public void shouldSerializeUnitParameters() throws IOException {
        final LogbookParametersSerializer logbookParametersSerializer = new LogbookParametersSerializer();
        final LogbookLifeCycleObjectGroupParameters params = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        assertNotNull(params);

        for (final LogbookParameterName value : LogbookParameterName.values()) {
            params.putParameterValue(value, value.name());
        }

        StringWriter stringJson = new StringWriter();
        JsonGenerator generator = new JsonFactory().createGenerator(stringJson);
        SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        logbookParametersSerializer.serialize(params, generator, serializerProvider);
        generator.flush();
        assertThat(stringJson.toString(), is(equalTo("{\"eventIdentifier\":\"eventIdentifier\",\"eventType\":\"eventType\"," +
                "\"eventDateTime\":\"eventDateTime\",\"eventIdentifierProcess\":\"eventIdentifierProcess\",\"eventTypeProcess\":\"eventTypeProcess\"," +
                "\"outcome\":\"outcome\",\"outcomeDetail\":\"outcomeDetail\",\"outcomeDetailMessage\":\"outcomeDetailMessage\"," +
                "\"agentIdentifier\":\"agentIdentifier\",\"agentIdentifierApplication\":\"agentIdentifierApplication\"," +
                "\"agentIdentifierApplicationSession\":\"agentIdentifierApplicationSession\",\"eventIdentifierRequest\":\"eventIdentifierRequest\"," +
                "\"agentIdentifierSubmission\":\"agentIdentifierSubmission\",\"agentIdentifierOriginating\":\"agentIdentifierOriginating\"," +
                "\"objectIdentifier\":\"objectIdentifier\",\"objectIdentifierRequest\":\"objectIdentifierRequest\"," +
                "\"objectIdentifierIncome\":\"objectIdentifierIncome\"}")));
    }



}