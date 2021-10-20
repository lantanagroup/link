package com.lantanagroup.link.serialize;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;

public class FhirJsonDeserializer extends JsonDeserializer<Resource> {

  @Override
  public Resource deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    String jsonContent = jsonParser.readValueAsTree().toString();
    FhirContext context = FhirContext.forR4();
    return (Resource) context.newJsonParser().parseResource(jsonContent);
  }
}
