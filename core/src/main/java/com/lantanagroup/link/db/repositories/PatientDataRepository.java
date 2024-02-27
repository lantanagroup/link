package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.mappers.PatientDataMapper;
import com.lantanagroup.link.db.mappers.PatientDataSizeMapper;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.db.model.PatientDataSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;

public class PatientDataRepository {
  private static final Logger logger = LoggerFactory.getLogger(PatientDataRepository.class);
  private static final PatientDataMapper mapper = new PatientDataMapper();
  private static final PatientDataSizeMapper sizeMapper = new PatientDataSizeMapper();
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

  public List<PatientData> findByReportIdAndPatientId(String reportId, String patientId) {
    String sql = "SELECT PD.* " +
            "FROM dbo.patientData AS PD " +
            "INNER JOIN dbo.reportPatientData AS RPD ON PD.patientId = RPD.patientId AND PD.resourceType = RPD.resourceType AND PD.resourceId = RPD.resourceId " +
            "WHERE RPD.reportId = :reportId AND RPD.patientId = :patientId;";
    Map<String, ?> parameters = Map.of("reportId", reportId, "patientId", patientId);
    return jdbc.query(sql, parameters, mapper);
  }

  public List<PatientDataSize> GetPatientDataResourceSize(String patientId, String resourceType) {
    String sql =  "SELECT pmr.patientId, pmr.resourceType, CAST(SUM(DATALENGTH(pmr.resource)) as FLOAT)/1024.0 as sizeKb FROM dbo.patientData AS pmr WHERE (:resourceType = '' OR resourceType = :resourceType) AND (:patientId = ''  OR patientId = :patientId) GROUP BY pmr.patientId, pmr.resourceType";

    if(resourceType == null) resourceType = "";
    if(patientId == null) patientId = "";

    HashMap<String, String> parameters = new HashMap();
    parameters.put("resourceType", resourceType);
    parameters.put("patientId", patientId);


    return jdbc.query(sql, parameters, sizeMapper);
  }

  public List<PatientDataSize> GetPatientDataResourceSizeInDateRange(String patientId, String resourceType, LocalDateTime startDate, LocalDateTime endDate) {
    String sql =  "SELECT pmr.patientId, pmr.resourceType, CAST(SUM(DATALENGTH(pmr.resource)) as FLOAT)/1024.0 as sizeKb FROM dbo.patientData AS pmr WHERE (:resourceType = '' OR resourceType = :resourceType) AND (:patientId = ''  OR patientId = :patientId) AND (:startDate IS NULL OR pmr.retrieved BETWEEN :startDate AND :endDate) GROUP BY pmr.patientId, pmr.resourceType";

    if(resourceType == null) resourceType = "";
    if(patientId == null) patientId = "";

    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("resourceType", resourceType);
    parameters.put("patientId", patientId);
    parameters.put("startDate", startDate);
    parameters.put("endDate", endDate);

    return jdbc.query(sql, parameters, sizeMapper);
  }

  public void saveAll(String reportId, List<PatientData> models) {
    String sql = "INSERT INTO dbo.patientData (id, dataTraceId, patientId, resourceType, resourceId, resource, retrieved) " +
            "SELECT :id, :dataTraceId, :patientId, :resourceType, :resourceId, :resource, :retrieved " +
            "WHERE NOT EXISTS (" +
            "    SELECT * FROM dbo.patientData " +
            "    WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId" +
            "); " +
            /* If the INSERT failed, this resource already exists for this patient; UPDATE instead */
            "IF @@ROWCOUNT = 0 " +
            "UPDATE dbo.patientData " +
            "SET dataTraceId = ISNULL(:dataTraceId, dataTraceId), resource = :resource, retrieved = :retrieved " +
            "WHERE patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId; " +
            /* Track this resource's association with the current report and patient */
            "INSERT INTO dbo.reportPatientData (reportId, patientId, resourceType, resourceId) " +
            "SELECT :reportId, :patientId, :resourceType, :resourceId " +
            "WHERE NOT EXISTS (" +
            "    SELECT * FROM dbo.reportPatientData " +
            "    WHERE reportId = :reportId AND patientId = :patientId AND resourceType = :resourceType AND resourceId = :resourceId" +
            ");";
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
              .map(model -> mapper.toParameters(reportId, model))
              .toArray(SqlParameterSource[]::new);
      if (parameters.length == 0) {
        break;
      }
      jdbc.batchUpdate(sql, parameters);
    }
  }

  public void beginReport(String reportId) {
    String sql = "DELETE FROM dbo.reportPatientData WHERE reportId = :reportId;";
    jdbc.update(sql, Map.of("reportId", reportId));
  }

  public void deleteAll() {
    {
      String sql = "DELETE FROM dbo.reportPatientData;";
      jdbc.update(sql, Map.of());
    }
    {
      String sql = "DELETE FROM dbo.patientData;";
      jdbc.update(sql, Map.of());
    }
  }

  public void deleteByPatientId(String patientId) {
    String sql = "DELETE FROM dbo.patientData WHERE patientId = :patientId;";
    Map<String, ?> parameters = Map.of("patientId", patientId);
    jdbc.update(sql, parameters);
  }

  public void deleteByReportId(String reportId) {
    {
      String sql = "DELETE FROM dbo.reportPatientData WHERE reportId = :reportId;";
      Map<String, ?> parameters = Map.of("reportId", reportId);
      jdbc.update(sql, parameters);
    }
    {
      String sql = "DELETE FROM dbo.patientData WHERE dataTraceId IN " +
              "(SELECT dataTraceId FROM dbo.dataTrace AS DT " +
              "INNER JOIN dbo.query AS Q ON DT.queryId = Q.id " +
              "WHERE Q.reportId = :reportId);";
      Map<String, ?> parameters = Map.of("reportId", reportId);
      jdbc.update(sql, parameters);
    }
  }

  public int deleteByRetrievedBefore(Date date) {
    String sql = "DELETE FROM dbo.patientData WHERE retrieved < :date;";
    Map<String, ?> parameters = Map.of("date", date);
    return jdbc.update(sql, parameters);
  }
}
