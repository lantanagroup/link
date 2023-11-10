package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.PatientDataMapper;
import com.lantanagroup.link.db.model.PatientData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
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

  public void saveAll(List<PatientData> models) {
    String sql = "INSERT INTO dbo.patientData (id, dataTraceId, patientId, resourceType, resourceId, resource, retrieved) " +
            "SELECT :id, :dataTraceId, :patientId, :resourceType, :resourceId, :resource, :retrieved " +
            "WHERE NOT EXISTS (" +
            "    SELECT * FROM dbo.patientData " +
            "    WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId" +
            "); " +
            "IF @@ROWCOUNT = 0 " +
            "UPDATE dbo.patientData " +
            "SET dataTraceId = ISNULL(:dataTraceId, dataTraceId), resource = :resource, retrieved = :retrieved " +
            "WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId;";
    int batchSize = 100;
    for (int batchIndex = 0; ; batchIndex++) {
      SqlParameterSource[] parameters = models.stream()
              .skip((long) batchIndex * batchSize)
              .limit(batchSize)
              .peek(model -> {
                if (model.getId() == null) {
                  model.setId(UUID.randomUUID());
                }
                if (model.getRetrieved() == null) {
                  model.setRetrieved(new Date());
                }
              })
              .map(mapper::toParameters)
              .toArray(SqlParameterSource[]::new);
      if (parameters.length == 0) {
        break;
      }
      jdbc.batchUpdate(sql, parameters);
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

  public void deleteByReportId(String reportId) {
    String sql = "DELETE FROM dbo.patientData WHERE dataTraceId IN " +
            "(SELECT dataTraceId FROM dbo.dataTrace AS DT " +
            "INNER JOIN dbo.query AS Q ON DT.queryId = Q.id " +
            "WHERE Q.reportId = :reportId);";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    jdbc.update(sql, parameters);
  }

  public int deleteByRetrievedBefore(Date date) {
    String sql = "DELETE FROM dbo.patientData WHERE retrieved < :date;";
    Map<String, ?> parameters = Map.of("date", date);
    return jdbc.update(sql, parameters);
  }
}
