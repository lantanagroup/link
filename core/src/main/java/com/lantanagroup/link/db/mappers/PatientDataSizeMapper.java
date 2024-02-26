package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.PatientDataSize;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientDataSizeMapper extends BaseMapper<PatientDataSize> {
  @Override
  protected PatientDataSize doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    var model = new PatientDataSize();
    model.setResourceType(row.getString("resourceType"));
    model.setPatientId(row.getString("patientId"));
    model.setSizeKb(row.getDouble("sizeKb"));

    return model;
  }

  @Override
  protected Parameters doToParameters(PatientDataSize model) {
    Parameters parameters = new Parameters();
    parameters.addString("reportId", model.getResourceType());
    parameters.addString("patientId", model.getPatientId());
    parameters.addValue("sizeKb", model.getSizeKb());
    return parameters;
  }
}
