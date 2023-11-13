package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.Query;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryMapper extends BaseMapper<Query> {
  @Override
  protected Query doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    Query model = new Query();
    model.setId(row.getUUID("id"));
    model.setReportId(row.getString("reportId"));
    model.setQueryType(row.getString("queryType"));
    model.setUrl(row.getString("url"));
    model.setBody(row.getString("body"));
    model.setRetrieved(row.getDate("retrieved"));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(Query model) {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addString("reportId", model.getReportId());
    parameters.addString("queryType", model.getQueryType());
    parameters.addString("url", model.getUrl());
    parameters.addString("body", model.getBody());
    parameters.addDate("retrieved", model.getRetrieved());
    return parameters;
  }
}
