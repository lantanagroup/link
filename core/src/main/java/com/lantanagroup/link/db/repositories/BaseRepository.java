package com.lantanagroup.link.db.repositories;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseRepository<T> {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final FhirContext fhirContext = FhirContextProvider.getFhirContext();

  protected String serializeList(List<?> list) {
    try {
      return objectMapper.writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  protected <U> List<U> deserializeList(Class<U> itemType, String json) {
    JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
    try {
      return objectMapper.readValue(json, listType);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  protected String serializeResource(IBaseResource resource) {
    return fhirContext.newJsonParser().encodeResourceToString(resource);

  }

  protected <U extends IBaseResource> U deserializeResource(Class<U> resourceType, String string) {
    return fhirContext.newJsonParser().parseResource(resourceType, string);
  }

  protected abstract T mapOne(ResultSet resultSet) throws SQLException;

  protected List<T> mapAll(ResultSet resultSet) throws SQLException {
    List<T> entities = new ArrayList<>();
    while (resultSet.next()) {
      entities.add(mapOne(resultSet));
    }
    return entities;
  }
}
