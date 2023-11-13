package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.BulkStatus;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BulkStatusMapper extends BaseMapper<BulkStatus> {
  @Override
  protected BulkStatus doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    BulkStatus model = new BulkStatus();
    model.setId(row.getUUID("id"));
    model.setStatusUrl(row.getString("statusUrl"));
    model.setStatus(row.getString("status"));
    model.setDate(row.getDate("date"));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(BulkStatus model) {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addString("statusUrl", model.getStatusUrl());
    parameters.addString("status", model.getStatus());
    parameters.addDate("date", model.getDate());
    return parameters;
  }
}
