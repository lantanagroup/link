package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.PatientMeasureReport;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientMeasureReportMapper extends BaseMapper<PatientMeasureReport> {
  @Override
  protected PatientMeasureReport doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    PatientMeasureReport model = new PatientMeasureReport();
    model.setId(row.getString("id"));
    model.setReportId(row.getString("reportId"));
    model.setMeasureId(row.getString("measureId"));
    model.setPatientId(row.getString("patientId"));
    model.setMeasureReport(row.getResource("measureReport", MeasureReport.class));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(PatientMeasureReport model) {
    Parameters parameters = new Parameters();
    parameters.addString("id", model.getId());
    parameters.addString("reportId", model.getReportId());
    parameters.addString("measureId", model.getMeasureId());
    parameters.addString("patientId", model.getPatientId());
    parameters.addResource("measureReport", model.getMeasureReport());
    return parameters;
  }
}
