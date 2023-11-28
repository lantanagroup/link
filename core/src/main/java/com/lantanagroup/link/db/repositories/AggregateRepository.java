package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.AggregateMapper;
import com.lantanagroup.link.db.model.Aggregate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class AggregateRepository {
  private static final AggregateMapper mapper = new AggregateMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public AggregateRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<Aggregate> findByReportId(String reportId) {
    String sql = "SELECT * FROM dbo.[aggregate] WHERE reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.query(sql, parameters, mapper);
  }

  private int insert(Aggregate model) {
    String sql = "INSERT INTO dbo.[aggregate] (id, reportId, measureId, report) " +
            "VALUES (:id, :reportId, :measureId, :report);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(Aggregate model) {
    String sql = "UPDATE dbo.[aggregate] " +
            "SET reportId = :reportId, measureId = :measureId, report = :report " +
            "WHERE id = :id;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void save(Aggregate model) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
    });
  }

  public void deleteByReportId(String reportId) {
    String sql = "DELETE FROM dbo.[aggregate] WHERE reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    jdbc.update(sql, parameters);
  }

  public void deleteAll() {
    String sql = "DELETE FROM dbo.[aggregate];";
    jdbc.update(sql, Map.of());
  }
}
