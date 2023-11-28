package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.StreamUtils;
import com.lantanagroup.link.db.mappers.PatientListMapper;
import com.lantanagroup.link.db.model.PatientList;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PatientListRepository {
  private static final PatientListMapper mapper = new PatientListMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public PatientListRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<PatientList> findAll() {
    String sql = "SELECT * FROM dbo.patientList;";
    return jdbc.query(sql, mapper);
  }

  public PatientList findById(UUID id) {
    String sql = "SELECT * FROM dbo.patientList WHERE id = :id;";
    Map<String, ?> parameters = Map.of("id", id);
    return jdbc.query(sql, parameters, mapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  public List<PatientList> findByReportId(String reportId) {
    String sql = "SELECT PL.* FROM dbo.patientList AS PL " +
            "INNER JOIN dbo.reportPatientList AS RPL ON PL.id = RPL.patientListId " +
            "WHERE RPL.reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.query(sql, parameters, mapper);
  }

  public PatientList findByMeasureIdAndReportingPeriod(String measureId, String periodStart, String periodEnd) {
    String sql = "SELECT * FROM dbo.patientList " +
            "WHERE measureId = :measureId AND periodStart = :periodStart AND periodEnd = :periodEnd;";
    Map<String, ?> parameters = Map.of(
            "measureId", measureId,
            "periodStart", periodStart,
            "periodEnd", periodEnd);
    return jdbc.query(sql, parameters, mapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  private int insert(PatientList model) {
    if (model.getId() == null) {
      model.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.patientList (id, measureId, periodStart, periodEnd, patients, lastUpdated) " +
            "VALUES (:id, :measureId, :periodStart, :periodEnd, :patients, :lastUpdated);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(PatientList model) {
    String sql = "UPDATE dbo.patientList SET patients = :patients, lastUpdated = :lastUpdated " +
            "WHERE measureId = :measureId AND periodStart = :periodStart AND periodEnd = :periodEnd;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void save(PatientList model) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
    });
  }

  public void deleteAll() {
    String sql = "DELETE FROM dbo.patientList;";
    jdbc.update(sql, Map.of());
  }

  public void deleteById(UUID id) {
    String sql = "DELETE FROM dbo.patientList WHERE id = :id;";
    Map<String, ?> parameters = Map.of("id", id);
    jdbc.update(sql, parameters);
  }

  public int deleteByLastUpdatedBefore(Date date) {
    String sql = "DELETE FROM dbo.patientList WHERE lastUpdated < :date;";
    Map<String, ?> parameters = Map.of("date", date);
    return jdbc.update(sql, parameters);
  }
}
