package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.db.model.PatientId;
import com.lantanagroup.link.db.model.PatientList;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientListMapper extends BaseMapper<PatientList> {
  @Override
  protected PatientList doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    PatientList model = new PatientList();
    model.setId(row.getUUID("id"));
    model.setMeasureId(row.getString("measureId"));
    model.setPeriodStart(row.getString("periodStart"));
    model.setPeriodEnd(row.getString("periodEnd"));
    model.setPatients(row.getJsonList("patients", PatientId.class));
    model.setLastUpdated(row.getDate("lastUpdated"));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(PatientList model) throws JsonProcessingException {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addString("measureId", model.getMeasureId());
    parameters.addString("periodStart", model.getPeriodStart());
    parameters.addString("periodEnd", model.getPeriodEnd());
    parameters.addJsonList("patients", model.getPatients());
    parameters.addDate("lastUpdated", model.getLastUpdated());
    return parameters;
  }
}
