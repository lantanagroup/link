package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ValidationResultCategoryMapper extends BaseMapper<ValidationResultCategory> {
  @Override
  protected ValidationResultCategory doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    ValidationResultCategory model = new ValidationResultCategory();
    model.setId(row.getUUID("id"));
    model.setValidationResultId(row.getUUID("validationResultId"));
    model.setCategoryCode(row.getString("categoryCode"));
    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(ValidationResultCategory model) throws JsonProcessingException {
    Parameters parameters = new Parameters();
    parameters.addUUID("id", model.getId());
    parameters.addUUID("validationResultId", model.getValidationResultId());
    parameters.addString("categoryCode", model.getCategoryCode());
    return parameters;
  }
}
