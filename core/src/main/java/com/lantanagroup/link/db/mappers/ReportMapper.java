package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.ReportStatuses;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportMapper extends BaseMapper<Report> {
  @Override
  protected Report doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    Report model = new Report();
    model.setId(row.getString("id"));
    model.setMeasureIds(row.getJsonList("measureIds", String.class));
    model.setPeriodStart(row.getString("periodStart"));
    model.setPeriodEnd(row.getString("periodEnd"));
    model.setStatus(ReportStatuses.valueOf(row.getString("status")));
    model.setVersion(row.getString("version"));
    model.setGeneratedTime(row.getDate("generatedTime"));
    model.setSubmittedTime(row.getDate("submittedTime"));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(Report model) throws JsonProcessingException {
    Parameters parameters = new Parameters();
    parameters.addString("id", model.getId());
    parameters.addJsonList("measureIds", model.getMeasureIds());
    parameters.addString("periodStart", model.getPeriodStart());
    parameters.addString("periodEnd", model.getPeriodEnd());
    parameters.addString("status", model.getStatus().name());
    parameters.addString("version", model.getVersion());
    parameters.addDate("generatedTime", model.getGeneratedTime());
    parameters.addDate("submittedTime", model.getSubmittedTime());
    return parameters;
  }
}
