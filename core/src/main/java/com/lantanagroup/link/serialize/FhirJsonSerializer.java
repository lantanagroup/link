package com.lantanagroup.link.serialize;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;

public class FhirJsonSerializer extends JsonSerializer<Resource> {

  @Override
  public Class<Resource> handledType() {
    return Resource.class;
  }

  @Override
  public void serialize(Resource resource, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    if (resource == null) {
      jsonGenerator.writeNull();
    } else {
      FhirContext ctx = FhirContext.forR4();
      jsonGenerator.writeRawValue(ctx.newJsonParser().encodeResourceToString(resource));
    }
  }
}
