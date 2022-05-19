package com.lantanagroup.link.serialize;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class FhirJsonDeserializer<T> extends JsonDeserializer<T> {
  private IParser jsonParser;

  public FhirJsonDeserializer(IParser jsonParser) {
    this.jsonParser = jsonParser;
  }

  @Override
  public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    String jsonContent = jsonParser.readValueAsTree().toString();
    return (T) this.jsonParser.parseResource(jsonContent);
  }
}
