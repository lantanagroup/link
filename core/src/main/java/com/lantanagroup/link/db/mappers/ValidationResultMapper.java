package com.lantanagroup.link.db.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ValidationResultMapper extends BaseMapper<ValidationResult> {
  final static String regex = "^\\d+:\\d+$";

  private String reportId;

  public ValidationResultMapper(String reportId) {
    this.reportId = reportId;
  }

  public ValidationResultMapper() {
  }

  public static List<ValidationResult> toValidationResults(OperationOutcome outcome) {
    return outcome.getIssue().stream()
            .map(ValidationResultMapper::toValidationResult)
            .collect(Collectors.toList());
  }

  /**
   * Convert an operation outcome issue to a persistable validation result. The validation libraries return an
   * OperationOutcome, and this method is used to convert the OO to a persistable model.
   */
  private static ValidationResult toValidationResult(OperationOutcome.OperationOutcomeIssueComponent model) {
    ValidationResult result = new ValidationResult();

    if (model.getCode() == OperationOutcome.IssueType.NULL) {
      result.setCode("NULL");
    } else {
      result.setCode(model.getCode().toCode());
    }

    result.setDetails(model.getDetails().getText());
    result.setSeverity(model.getSeverity().toCode());
    result.setExpression(getExpression(model));
    result.setPosition(getPosition(model));

    return result;
  }

  /**
   * The REST API responds with an OperationOutcome,
   * so this method is used to convert the persisted validation results into an OperationOutcome
   * that can be returned by the REST API.
   */
  public static OperationOutcome toOperationOutcome(List<ValidationResult> results) {
    OperationOutcome outcome = new OperationOutcome();

    if (results != null && !results.isEmpty()) {
      for (ValidationResult result : results) {
        outcome.addIssue(toOperationOutcomeIssue(result));
      }
    } else {
      outcome.addIssue()
              .setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
              .setCode(OperationOutcome.IssueType.INFORMATIONAL)
              .setDetails(new CodeableConcept().setText("No issues found"));
    }

    return outcome;
  }

  public static Parameters getParameters(String reportId, String code) {
    Parameters parameters = new Parameters();
    parameters.addString("reportId", reportId);
    parameters.addString("code", code);
    return parameters;
  }

  /**
   * Convert a validation result to an operation outcome issue. Used by the toOperationOutcome method.
   */
  private static OperationOutcome.OperationOutcomeIssueComponent toOperationOutcomeIssue(ValidationResult result) {
    OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
    String code = result.getCode();
    if (!StringUtils.equals(code, "NULL")) {
      issue.setCode(OperationOutcome.IssueType.fromCode(code));
    }
    issue.setSeverity(OperationOutcome.IssueSeverity.fromCode(result.getSeverity()));
    issue.getDetails().setText(result.getDetails());

    if (result.getExpression() != null && result.getPosition() != null) {
      issue.getExpression().add(new StringType(result.getExpression()));
      issue.getExpression().add(new StringType(result.getPosition()));
    } else if (result.getExpression() != null) {
      issue.getExpression().add(new StringType(result.getExpression()));
    } else if (result.getPosition() != null) {
      issue.getExpression().add(new StringType(result.getPosition()));
    }

    return issue;
  }

  /**
   * The expression field is used to store the path to the element that caused the issue. The model's expression field
   * contains both the path and the position of the element. This method returns the path portion of the expression.
   *
   * @param model
   * @return
   */
  private static String getExpression(OperationOutcome.OperationOutcomeIssueComponent model) {
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
  private static String getPosition(OperationOutcome.OperationOutcomeIssueComponent model) {
    if (model.getExpression().size() == 2) {
      return model.getExpression().get(1).asStringValue();
    } else if (model.getExpression().size() == 1 && Pattern.matches(regex, model.getExpression().get(0).asStringValue())) {
      return model.getExpression().get(0).asStringValue();
    }
    return null;
  }

  @Override
  protected ValidationResult doToModel(ResultSet resultSet) throws JsonProcessingException, SQLException {
    Row row = new Row(resultSet);
    ValidationResult model = new ValidationResult();

    model.setId(row.getUUID("id"));
    model.setReportId(row.getString("reportId"));
    model.setCode(row.getString("code"));
    model.setDetails(row.getString("details"));
    model.setSeverity(row.getString("severity"));
    model.setExpression(row.getString("expression"));
    model.setPosition(row.getString("position"));

    return model;
  }

  @Override
  protected SqlParameterSource doToParameters(ValidationResult model) throws JsonProcessingException {
    Parameters parameters = new Parameters();

    parameters.addUUID("id", model.getId());
    parameters.addString("reportId", this.reportId);
    parameters.addString("code", model.getCode());
    parameters.addString("details", model.getDetails());
    parameters.addString("severity", model.getSeverity());
    parameters.addString("expression", model.getExpression());
    parameters.addString("position", model.getPosition());

    return parameters;
  }
}
