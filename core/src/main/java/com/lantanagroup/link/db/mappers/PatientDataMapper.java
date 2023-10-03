package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.PatientData;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientDataMapper extends BaseMapper<PatientData> {
  @Override
  protected PatientData doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    PatientData model = new PatientData();
    model.setId(row.getUUID("id"));
    model.setPatientId(row.getString("patientId"));
    model.setResourceType(row.getString("resourceType"));
    model.setResourceId(row.getString("resourceId"));
    model.setResource(row.getResource("resource"));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(PatientData model) {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addString("patientId", model.getPatientId());
    parameters.addString("resourceType", model.getResourceType());
    parameters.addString("resourceId", model.getResourceId());
    parameters.addResource("resource", model.getResource());
    return parameters;
  }
}
