package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class ValidationResultMapper extends BaseMapper<OperationOutcome.OperationOutcomeIssueComponent> {
  final String regex = "^\\d+:\\d+$";

  private String reportId;

  public ValidationResultMapper(String reportId) {
    this.reportId = reportId;
  }

  public ValidationResultMapper() {
  }

  @Override
  protected OperationOutcome.OperationOutcomeIssueComponent doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    OperationOutcome.OperationOutcomeIssueComponent model = new OperationOutcome.OperationOutcomeIssueComponent();

    model.setCode(OperationOutcome.IssueType.fromCode(row.getString("code")));
    model.setSeverity(OperationOutcome.IssueSeverity.fromCode(row.getString("severity")));
    model.setDetails(new CodeableConcept().setText(row.getString("details")));

    String expression = row.getString("expression");
    String position = row.getString("position");

    if (expression != null && position != null) {
      model.getExpression().add(new StringType(expression));
      model.getExpression().add(new StringType(position));
    } else if (expression != null) {
      model.getExpression().add(new StringType(expression));
    } else if (position != null) {
      model.getExpression().add(new StringType(position));
    }

    return model;
  }

  /**
   * The expression field is used to store the path to the element that caused the issue. The model's expression field
   * contains both the path and the position of the element. This method returns the path portion of the expression.
   *
   * @param model
   * @return
   */
  private String getExpression(OperationOutcome.OperationOutcomeIssueComponent model) {
    if (model.getExpression().size() == 2) {
      return model.getExpression().get(0).asStringValue();
    } else if (model.getExpression().size() == 1 && !Pattern.matches(regex, model.getExpression().get(0).asStringValue())) {
      return model.getExpression().get(0).asStringValue();
    }
    return null;
  }

  /**
   * The position field is used to store the row/col to the element that caused the issue. The model's expression field
   * contains both the path and the position of the element. This method returns the position portion of the expression.
   *
   * @param model
   * @return
   */
  private String getPosition(OperationOutcome.OperationOutcomeIssueComponent model) {
    if (model.getExpression().size() == 2) {
      return model.getExpression().get(1).asStringValue();
    } else if (model.getExpression().size() == 1 && Pattern.matches(regex, model.getExpression().get(0).asStringValue())) {
      return model.getExpression().get(0).asStringValue();
    }
    return null;
  }

  @Override
  protected SqlParameterSource doToParameters(OperationOutcome.OperationOutcomeIssueComponent model) throws JsonProcessingException {
    Parameters parameters = new Parameters();

    if (this.reportId != null) {
      parameters.addString("reportId", this.reportId);
    }

    parameters.addString("code", model.getCode().toCode());
    parameters.addString("severity", model.getSeverity().toCode());
    parameters.addString("details", model.getDetails().getText());
    parameters.addString("expression", this.getExpression(model));
    parameters.addString("position", this.getPosition(model));

    return parameters;
  }
}
