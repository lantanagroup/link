package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.StreamUtils;
import com.lantanagroup.link.db.mappers.ConceptMapMapper;
import com.lantanagroup.link.db.model.ConceptMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class ConceptMapRepository {
  private static final ConceptMapMapper mapper = new ConceptMapMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public ConceptMapRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<ConceptMap> findAll() {
    String sql = "SELECT * FROM dbo.conceptMap;";
    return jdbc.query(sql, mapper);
  }

  public ConceptMap findById(String id) {
    String sql = "SELECT * FROM dbo.conceptMap WHERE id = :id;";
    Map<String, ?> parameters = Map.of("id", id);
    return jdbc.query(sql, parameters, mapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  private int insert(ConceptMap model) {
    String sql = "INSERT INTO dbo.conceptMap (id, contexts, conceptMap) VALUES (:id, :contexts, :conceptMap);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(ConceptMap model) {
    String sql = "UPDATE dbo.conceptMap SET contexts = :contexts, conceptMap = :conceptMap WHERE id = :id;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void save(ConceptMap model) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
    });
  }
}
