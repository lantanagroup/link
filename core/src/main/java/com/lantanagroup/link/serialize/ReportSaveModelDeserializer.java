package com.lantanagroup.link.serialize;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;

public class ReportSaveModelDeserializer extends JsonDeserializer<Resource>{
    FhirContext ctx = FhirContextProvider.getFhirContext();

    @Override
    public Resource deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String json = objectMapper.writeValueAsString(node);

        return (Resource) ctx.newJsonParser().parseResource(json);
    }
}