package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.ValidationResultMapper;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ValidationRepository {
  private static final ValidationResultMapper mapper = new ValidationResultMapper();

  private final NamedParameterJdbcTemplate jdbc;

  public ValidationRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    TransactionTemplate txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<OperationOutcome.OperationOutcomeIssueComponent> findValidationResults(String reportId, OperationOutcome.IssueSeverity severity) {
    String sql = "SELECT * FROM dbo.[validationResult] WHERE reportId = :reportId";

    if (severity != null) {
      // Look for validation results based on the severity, if specified. Error severity includes only errors. Warning
      // severity includes both errors and warnings. Information severity includes all three.
      switch (severity) {
        case FATAL:
          sql += " AND severity = 'fatal'";
          break;
        case ERROR:
          sql += " AND severity in ('error', 'fatal')";
          break;
        case WARNING:
          sql += " AND severity in ('warning', 'error', 'fatal')";
          break;
        case INFORMATION:
        case NULL:
          break;
        default:
          throw new IllegalArgumentException("Invalid severity: " + severity);
      }
    }

    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.query(sql, parameters, mapper);
  }

  public int insertAll(String reportId, List<OperationOutcome.OperationOutcomeIssueComponent> models) {
    ValidationResultMapper mapperWithReportId = new ValidationResultMapper(reportId);
    String sql = "INSERT INTO dbo.validationResult (reportId, code, details, severity, expression, position) " +
            "VALUES (:reportId, :code, :details, :severity, :expression, :position);";
    SqlParameterSource[] parameters = models.stream()
            .map(mapperWithReportId::toParameters)
            .toArray(SqlParameterSource[]::new);
    if (parameters.length == 0) {
      return 0;
    }
    return Arrays.stream(jdbc.batchUpdate(sql, parameters)).sum();
  }

  public int deleteByReport(String reportId) {
    String sql = "DELETE FROM dbo.validationResult WHERE reportId = :reportId;";
    return jdbc.update(sql, Map.of("reportId", reportId));
  }
}
