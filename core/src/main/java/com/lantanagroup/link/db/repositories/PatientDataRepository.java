package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.PatientDataMapper;
import com.lantanagroup.link.db.model.PatientData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PatientDataRepository {
  private static final Logger logger = LoggerFactory.getLogger(PatientDataRepository.class);
  private static final PatientDataMapper mapper = new PatientDataMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public PatientDataRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<PatientData> findByPatientId(String patientId) {
    String sql = "SELECT * FROM dbo.patientData WHERE patientId = :patientId;";
    Map<String, ?> parameters = Map.of("patientId", patientId);
    return jdbc.query(sql, parameters, mapper);
  }

  private void upsert(PatientData model) {
    if (model.getId() == null) {
      model.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.patientData (id, patientId, resourceType, resourceId, resource, retrieved) " +
            "SELECT :id, :patientId, :resourceType, :resourceId, :resource, :retrieved " +
            "WHERE NOT EXISTS (" +
            "    SELECT * FROM dbo.patientData " +
            "    WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId" +
            "); " +
            "IF @@ROWCOUNT = 0 " +
            "UPDATE dbo.patientData " +
            "SET resource = :resource, retrieved = :retrieved " +
            "WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId;";
    jdbc.update(sql, mapper.toParameters(model));
  }

  private int insert(PatientData model) {
    if (model.getId() == null) {
      model.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.patientData (id, patientId, resourceType, resourceId, resource, retrieved) " +
            "VALUES (:id, :patientId, :resourceType, :resourceId, :resource, :retrieved);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(PatientData model) {
    String sql = "UPDATE dbo.patientData " +
            "SET resource = :resource, retrieved = :retrieved " +
            "WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void saveAll(List<PatientData> models) {
    for (PatientData model : models) {
      try {
        // Try a non-transacted insert-first strategy, which is not 100% safe
        // Simultaneous queries could satisfy the WHERE before attempting the INSERT (I think)
        // An intervening DELETE could occur (between a failed INSERT and the subsequent UPDATE)
        // Both of these situations will (correctly) result in an unhandled exception
        upsert(model);
      } catch (DataAccessException e) {
        logger.error("Error in patient data upsert", e);
        // Fall back to a transacted update-first strategy
        txTemplate.executeWithoutResult(tx -> {
          if (update(model) == 0) {
            insert(model);
          }
        });
      }
    }
  }

  public void deleteAll() {
    String sql = "TRUNCATE TABLE dbo.patientData;";
    jdbc.update(sql, Map.of());
  }

  public void deleteByPatientId(String patientId) {
    String sql = "DELETE FROM dbo.patientData WHERE patientId = :patientId;";
    Map<String, ?> parameters = Map.of("patientId", patientId);
    jdbc.update(sql, parameters);
  }

  public int deleteByRetrievedBefore(Date date) {
    String sql = "DELETE FROM dbo.patientData WHERE retrieved < :date;";
    Map<String, ?> parameters = Map.of("date", date);
    return jdbc.update(sql, parameters);
  }
}
