package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.QueryMapper;
import com.lantanagroup.link.db.model.Query;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class QueryRepository {
  private static final QueryMapper mapper = new QueryMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public QueryRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public int insert(Query model) {
    if (model.getId() == null) {
      model.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.query (id, reportId, queryType, url, body, retrieved) " +
            "VALUES (:id, :reportId, :queryType, :url, :body, :retrieved);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public int deleteAll() {
    String sql = "DELETE FROM dbo.query;";
    return jdbc.update(sql, Map.of());
  }

  public int deleteByReportId(String reportId) {
    String sql = "DELETE FROM dbo.query WHERE reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.update(sql, parameters);
  }

  public int deleteByRetrievedBefore(Date date) {
    String sql = "DELETE FROM dbo.query WHERE retrieved < :date;";
    Map<String, ?> parameters = Map.of("date", date);
    return jdbc.update(sql, parameters);
  }
}
