package com.lantanagroup.link.db.mappers;

import com.lantanagroup.link.db.model.DataTrace;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataTraceMapper extends BaseMapper<DataTrace> {
  @Override
  protected DataTrace doToModel(ResultSet resultSet) throws SQLException {
    Row row = new Row(resultSet);
    DataTrace model = new DataTrace();
    model.setId(row.getUUID("id"));
    model.setQueryId(row.getUUID("queryId"));
    model.setPatientId(row.getString("patientId"));
    model.setResourceType(row.getString("resourceType"));
    model.setResourceId(row.getString("resourceId"));
    model.setOriginalResource(row.getString("originalResource"));
    return model;
  }

  @Override
  protected Parameters doToParameters(DataTrace model) {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addUUID("queryId", model.getQueryId());
    parameters.addString("patientId", model.getPatientId());
    parameters.addString("resourceType", model.getResourceType());
    parameters.addString("resourceId", model.getResourceId());
    parameters.addString("originalResource", model.getOriginalResource());
    return parameters;
  }
}
