package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.model.BulkResponse;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BulkStatusResultMapper extends BaseMapper<BulkStatusResult> {
  @Override
  protected BulkStatusResult doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    BulkStatusResult model = new BulkStatusResult();
    model.setId(row.getUUID("id"));
    model.setStatusId(row.getUUID("statusId"));
    model.setResult(row.getJsonObject("result", BulkResponse.class));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(BulkStatusResult model) throws JsonProcessingException {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addUUID("statusId", model.getStatusId());
    parameters.addJsonObject("result", model.getResult());
    return parameters;
  }
}
