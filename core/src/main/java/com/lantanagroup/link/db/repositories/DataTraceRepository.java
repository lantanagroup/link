package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.DataTraceMapper;
import com.lantanagroup.link.db.model.DataTrace;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataTraceRepository {
  private static final DataTraceMapper mapper = new DataTraceMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public DataTraceRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public int insertAll(List<DataTrace> models) {
    String sql = "INSERT INTO dbo.dataTrace (id, queryId, patientId, resourceType, resourceId) " +
            "VALUES (:id, :queryId, :patientId, :resourceType, :resourceId);";
    SqlParameterSource[] parameters = models.stream()
            .map(mapper::toParameters)
            .toArray(SqlParameterSource[]::new);
    if (parameters.length == 0) {
      return 0;
    }
    return Arrays.stream(jdbc.batchUpdate(sql, parameters)).sum();
  }

  public int deleteAll() {
    String sql = "DELETE FROM dbo.dataTrace;";
    return jdbc.update(sql, Map.of());
  }

  public int deleteUnreferenced() {
    String sql = "DELETE FROM dbo.dataTrace WHERE id NOT IN " +
            "(SELECT dataTraceId FROM dbo.patientData WHERE dataTraceId IS NOT NULL);";
    return jdbc.update(sql, Map.of());
  }
}
