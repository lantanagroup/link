package com.lantanagroup.link.db.mappers;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public abstract class BaseMapper<T> implements RowMapper<T> {
  protected static final FhirContext fhirContext = FhirContextProvider.getFhirContext();
  protected static final ObjectMapper objectMapper = new ObjectMapper();

  public T toModel(ResultSet resultSet) throws SQLException {
    try {
      return doToModel(resultSet);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract T doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException;

  public SqlParameterSource toParameters(T model) {
    try {
      return doToParameters(model);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract SqlParameterSource doToParameters(T model) throws JsonProcessingException;

  @Override
  public T mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
    return toModel(rs);
  }

  protected static class Row {
    private final ResultSet resultSet;

    public Row(ResultSet resultSet) {
      this.resultSet = resultSet;
    }

    public String getString(String columnName) throws SQLException {
      return resultSet.getNString(columnName);
    }

    public Date getDate(String columnName) throws SQLException {
      return resultSet.getTimestamp(columnName);
    }

    public UUID getUUID(String columnName) throws SQLException {
      return resultSet.getObject(columnName, UUID.class);
    }

    public <U extends IBaseResource> U getResource(String columnName, Class<U> type) throws SQLException {
      String json = getString(columnName);
      return fhirContext.newJsonParser().parseResource(type, json);
    }

    public IBaseResource getResource(String columnName) throws SQLException {
      String json = getString(columnName);
      return fhirContext.newJsonParser().parseResource(json);
    }

    public <U> U getJsonObject(String columnName, Class<U> type) throws JsonProcessingException, SQLException {
      String json = getString(columnName);
      return objectMapper.readValue(json, type);
    }

    public <U> List<U> getJsonList(String columnName, Class<U> type) throws JsonProcessingException, SQLException {
      String json = getString(columnName);
      JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
      return objectMapper.readValue(json, javaType);
    }
  }

  protected static class Parameters extends MapSqlParameterSource {
    public void addString(String name, String value) {
      addValue(name, value, Types.NVARCHAR);
    }

    public void addDate(String name, Date value) {
      addValue(name, value, Types.TIMESTAMP);
    }

    public void addUUID(String name, UUID value) {
      addValue(name, value);
    }

    public void addResource(String name, IBaseResource value) {
      String json = fhirContext.newJsonParser().encodeResourceToString(value);
      addString(name, json);
    }

    public void addJsonObject(String name, Object value) throws JsonProcessingException {
      String json = objectMapper.writeValueAsString(value);
      addString(name, json);
    }

    public void addJsonList(String name, List<?> value) throws JsonProcessingException {
      addJsonObject(name, value);
    }
  }
}
