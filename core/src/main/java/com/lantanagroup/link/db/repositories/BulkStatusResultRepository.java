package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.BulkStatusResultMapper;
import com.lantanagroup.link.db.model.BulkStatusResult;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class BulkStatusResultRepository {
  private static final BulkStatusResultMapper mapper = new BulkStatusResultMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public BulkStatusResultRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<BulkStatusResult> findAll() {
    String sql = "SELECT * FROM dbo.bulkStatusResult;";
    return jdbc.query(sql, mapper);
  }

  private int insert(BulkStatusResult model) {
    if (model.getId() == null) {
      model.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.bulkStatusResult (id, statusId, result) " +
            "VALUES (:id, :statusId, :result);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(BulkStatusResult model) {
    String sql = "UPDATE dbo.bulkStatusResult " +
            "SET statusId = :statusId, result = :result " +
            "WHERE id = :id;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void save(BulkStatusResult model) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
    });
  }
}
