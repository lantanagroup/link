package com.lantanagroup.link.serialize;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


public class FhirJsonDeserializer<T> extends JsonDeserializer<T> {
  private final IParser jsonParser;

  public FhirJsonDeserializer(IParser jsonParser) {
    this.jsonParser = jsonParser;
  }

  @Override
  public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    try {
      String jsonContent = jsonParser.readValueAsTree().toString();
      return (T) this.jsonParser.parseResource(jsonContent);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
