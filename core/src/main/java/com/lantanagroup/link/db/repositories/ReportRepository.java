package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.StreamUtils;
import com.lantanagroup.link.db.mappers.ReportMapper;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.Report;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportRepository {
  private static final ReportMapper mapper = new ReportMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public ReportRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<Report> findAll() {
    String sql = "SELECT * FROM dbo.report;";
    return jdbc.query(sql, mapper);
  }

  public Report findById(String id) {
    String sql = "SELECT * FROM dbo.report WHERE id = :id;";
    Map<String, ?> parameters = Map.of("id", id);
    return jdbc.query(sql, parameters, mapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  public List<Report> findByPatientListId(UUID patientListId) {
    String sql = "SELECT R.* FROM dbo.report AS R " +
            "INNER JOIN dbo.reportPatientList AS RPL ON R.id = RPL.reportId " +
            "WHERE RPL.patientListId = :patientListId;";
    Map<String, ?> parameters = Map.of("patientListId", patientListId);
    return jdbc.query(sql, parameters, mapper);
  }

  private int insert(Report model) {
    String sql = "INSERT INTO dbo.report (id, measureIds, periodStart, periodEnd, status, version, generatedTime, submittedTime) " +
            "VALUES (:id, :measureIds, :periodStart, :periodEnd, :status, :version, :generatedTime, :submittedTime);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(Report model) {
    String sql = "UPDATE dbo.report " +
            "SET measureIds = :measureIds, periodStart = :periodStart, periodEnd = :periodEnd, status = :status, version = :version, generatedTime = :generatedTime, submittedTime = :submittedTime " +
            "WHERE id = :id;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private void savePatientListIds(String reportId, List<UUID> patientListIds) {
    {
      String sql = "DELETE FROM dbo.reportPatientList WHERE reportId = :reportId;";
      Map<String, ?> parameters = Map.of("reportId", reportId);
      jdbc.update(sql, parameters);
    }
    for (UUID patientListId : patientListIds) {
      String sql = "INSERT INTO dbo.reportPatientList (reportId, patientListId) VALUES (:reportId, :patientListId);";
      Map<String, ?> parameters = Map.of(
              "reportId", reportId,
              "patientListId", patientListId);
      jdbc.update(sql, parameters);
    }
  }

  public void save(Report model, List<PatientList> patientLists) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
      if (patientLists != null) {
        List<UUID> patientListIds = patientLists.stream()
                .map(PatientList::getId)
                .collect(Collectors.toList());
        savePatientListIds(model.getId(), patientListIds);
      }
    });
  }

  public void save(Report model) {
    save(model, null);
  }

  public void deleteById(String id) {
    txTemplate.executeWithoutResult(tx -> {
      savePatientListIds(id, List.of());
      String sql = "DELETE FROM dbo.report WHERE id = :id;";
      Map<String, ?> parameters = Map.of("id", id);
      jdbc.update(sql, parameters);
    });
  }

  public void deleteAll() {
    txTemplate.executeWithoutResult(tx -> {
      {
        String sql = "TRUNCATE TABLE dbo.reportPatientList;";
        jdbc.update(sql, Map.of());
      }
      {
        String sql = "TRUNCATE TABLE dbo.report;";
        jdbc.update(sql, Map.of());
      }
    });
  }
}
