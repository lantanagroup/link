package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.Aggregate;
import org.hl7.fhir.r4.model.MeasureReport;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AggregateMapper extends BaseMapper<Aggregate> {
  @Override
  protected Aggregate doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    Aggregate model = new Aggregate();
    model.setId(row.getString("id"));
    model.setReportId(row.getString("reportId"));
    model.setMeasureId(row.getString("measureId"));
    model.setReport(row.getResource("report", MeasureReport.class));
    return model;
  }

  @Override
  protected Parameters doToParameters(Aggregate model) {
    Parameters parameters = new Parameters();
    parameters.addString("id", model.getId());
    parameters.addString("reportId", model.getReportId());
    parameters.addString("measureId", model.getMeasureId());
    parameters.addResource("report", model.getReport());
    return parameters;
  }
}
