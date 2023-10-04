package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.QueryMapper;
import com.lantanagroup.link.db.model.Query;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
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
    String sql = "INSERT INTO dbo.Query (id, reportId, queryType, url, body, retrieved) " +
            "VALUES (:id, :reportId, :queryType, :url, :body, :retrieved);";
    return jdbc.update(sql, mapper.toParameters(model));
  }
}
