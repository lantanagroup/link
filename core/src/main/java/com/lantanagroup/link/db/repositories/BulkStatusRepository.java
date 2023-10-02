package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.StreamUtils;
import com.lantanagroup.link.db.mappers.BulkStatusMapper;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatuses;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BulkStatusRepository {
  private static final BulkStatusMapper mapper = new BulkStatusMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public BulkStatusRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<BulkStatus> findAll() {
    String sql = "SELECT * FROM dbo.bulkStatus;";
    return jdbc.query(sql, mapper);
  }

  public BulkStatus findById(UUID id) {
    String sql = "SELECT * FROM dbo.bulkStatus WHERE id = :id;";
    Map<String, ?> parameters = Map.of("id", id);
    return jdbc.query(sql, parameters, mapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  public List<BulkStatus> findPendingWithUrl() {
    String sql = "SELECT * FROM dbo.bulkStatus WHERE statusUrl IS NOT NULL AND status = :status;";
    Map<String, ?> parameters = Map.of("status", BulkStatuses.PENDING);
    return jdbc.query(sql, parameters, mapper);
  }

  private int insert(BulkStatus model) {
    if (model.getId() == null) {
      model.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.bulkStatus (id, statusUrl, status, date) " +
            "VALUES (:id, :statusUrl, :status, :date);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(BulkStatus model) {
    String sql = "UPDATE dbo.bulkStatus " +
            "SET statusUrl = :statusUrl, status = :status, date = :date " +
            "WHERE id = :id;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void save(BulkStatus model) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
    });
  }
}
