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
import java.util.Date;
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

  public int deleteByReportId(String reportId) {
    String sql = "DELETE DT FROM dbo.dataTrace AS DT " +
            "INNER JOIN dbo.query AS Q ON DT.queryId = Q.id " +
            "WHERE Q.reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.update(sql, parameters);
  }

  public int deleteByRetrievedBefore(Date date) {
    String sql = "DELETE DT FROM dbo.dataTrace AS DT " +
            "INNER JOIN dbo.query AS Q ON DT.queryId = Q.id " +
            "WHERE Q.retrieved < :date;";
    Map<String, ?> parameters = Map.of("date", date);
    return jdbc.update(sql, parameters);
  }
}
