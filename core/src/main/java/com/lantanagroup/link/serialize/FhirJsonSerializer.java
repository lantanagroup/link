package com.lantanagroup.link.serialize;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;

public class FhirJsonSerializer<T extends IBaseResource> extends JsonSerializer<T> {
  private IParser jsonParser;
  private Class<T> theClass;

  public FhirJsonSerializer(IParser jsonParser, Class<T> theClass) {
    this.jsonParser = jsonParser;
    this.theClass = theClass;
  }

  @Override
  public Class<T> handledType() {
    return theClass;
  }

  @Override
  public void serialize(T resource, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    if (resource == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeRawValue(this.jsonParser.encodeResourceToString(resource));
    }
  }
}
