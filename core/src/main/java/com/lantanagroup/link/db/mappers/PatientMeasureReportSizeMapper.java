package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.PatientMeasureReportSize;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientMeasureReportSizeMapper extends BaseMapper<PatientMeasureReportSize> {
  @Override
  protected PatientMeasureReportSize doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    var model = new PatientMeasureReportSize();
    model.setReportId(row.getString("reportId"));
    model.setMeasureId(row.getString("measureId"));
    model.setPatientId(row.getString("patientId"));
    model.setSizeKb(row.getDouble("sizeKb"));

    return model;
  }

  @Override
  protected Parameters doToParameters(PatientMeasureReportSize model) {
    Parameters parameters = new Parameters();
    parameters.addString("reportId", model.getReportId());
    parameters.addString("measureId", model.getMeasureId());
    parameters.addString("patientId", model.getPatientId());
    parameters.addValue("sizeKb", model.getSizeKb());
    return parameters;
  }
}
