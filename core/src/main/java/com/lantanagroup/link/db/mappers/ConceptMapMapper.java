package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.db.model.ConceptMap;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ConceptMapMapper extends BaseMapper<ConceptMap> {
  @Override
  protected ConceptMap doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    ConceptMap model = new ConceptMap();
    model.setId(row.getString("id"));
    model.setContexts(row.getJsonList("contexts", String.class));
    model.setConceptMap(row.getResource("conceptMap", org.hl7.fhir.r4.model.ConceptMap.class));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(ConceptMap model) throws JsonProcessingException {
    Parameters parameters = new Parameters();
    parameters.addString("id", model.getId());
    parameters.addJsonList("contexts", model.getContexts());
    parameters.addResource("conceptMap", model.getConceptMap());
    return parameters;
  }
}
