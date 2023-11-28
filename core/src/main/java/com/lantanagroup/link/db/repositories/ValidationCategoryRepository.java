package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.ValidationResultCategoryMapper;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class ValidationCategoryRepository {
  private final ValidationResultCategoryMapper mapper = new ValidationResultCategoryMapper();

  private final NamedParameterJdbcTemplate jdbc;

  public ValidationCategoryRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    TransactionTemplate txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    this.jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<ValidationResultCategory> findByReportId(String reportId) {
    String sql = "SELECT * FROM dbo.[validationResultCategory] WHERE validationResultId in (SELECT id FROM dbo.validationResult WHERE reportId = :reportId);";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.query(sql, parameters, mapper);
  }

  public int insertAll(List<ValidationResultCategory> models) {
    String sql = "INSERT INTO dbo.validationResultCategory (validationResultId, categoryCode) " +
            "VALUES (:validationResultId, :categoryCode);";
    return jdbc.batchUpdate(sql, models.stream().map(mapper::toParameters).toArray(SqlParameterSource[]::new)).length;
  }

  public void deleteForReport(String reportId) {
    String sql = "DELETE FROM dbo.validationResultCategory WHERE validationResultId IN " +
            "(SELECT id FROM dbo.validationResult WHERE reportId = :reportId);";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    jdbc.update(sql, parameters);
  }
}
