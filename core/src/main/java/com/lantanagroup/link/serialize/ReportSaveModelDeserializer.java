package com.lantanagroup.link.serialize;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.IOException;
import java.text.ParseException;

public class ReportSaveModelDeserializer extends JsonDeserializer<Resource>{
    FhirContext ctx = FhirContext.forR4();

    @Override
    public Resource deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String json = objectMapper.writeValueAsString(node);

        return (Resource) ctx.newJsonParser().parseResource(json);
    }
}