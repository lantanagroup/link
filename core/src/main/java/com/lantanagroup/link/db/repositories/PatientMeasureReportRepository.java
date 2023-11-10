package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.StreamUtils;
import com.lantanagroup.link.db.mappers.PatientMeasureReportMapper;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class PatientMeasureReportRepository {
  private static final PatientMeasureReportMapper mapper = new PatientMeasureReportMapper();

  private final TransactionTemplate txTemplate;
  private final NamedParameterJdbcTemplate jdbc;

  public PatientMeasureReportRepository(DataSource dataSource, PlatformTransactionManager txManager) {
    txTemplate = new TransactionTemplate(txManager);
    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public PatientMeasureReport findById(String id) {
    String sql = "SELECT * FROM dbo.patientMeasureReport WHERE id = :id;";
    Map<String, ?> parameters = Map.of("id", id);
    return jdbc.query(sql, parameters, mapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  public List<PatientMeasureReport> findByReportId(String reportId) {
    String sql = "SELECT * FROM dbo.patientMeasureReport WHERE reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    return jdbc.query(sql, parameters, mapper);
  }

  public List<PatientMeasureReport> findByReportIdAndMeasureId(String reportId, String measureId) {
    String sql = "SELECT * FROM dbo.patientMeasureReport WHERE reportId = :reportId AND measureId = :measureId;";
    Map<String, ?> parameters = Map.of(
            "reportId", reportId,
            "measureId", measureId);
    return jdbc.query(sql, parameters, mapper);
  }

  private int insert(PatientMeasureReport model) {
    String sql = "INSERT INTO dbo.patientMeasureReport (id, reportId, measureId, patientId, measureReport) " +
            "VALUES (:id, :reportId, :measureId, :patientId, :measureReport);";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  private int update(PatientMeasureReport model) {
    String sql = "UPDATE dbo.patientMeasureReport " +
            "SET reportId = :reportId, measureId = :measureId, patientId = :patientId, measureReport = :measureReport " +
            "WHERE id = :id;";
    return jdbc.update(sql, mapper.toParameters(model));
  }

  public void save(PatientMeasureReport model) {
    txTemplate.executeWithoutResult(tx -> {
      if (update(model) == 0) {
        insert(model);
      }
    });
  }

  public void deleteByReportId(String reportId) {
    String sql = "DELETE FROM dbo.patientMeasureReport WHERE reportId = :reportId;";
    Map<String, ?> parameters = Map.of("reportId", reportId);
    jdbc.update(sql, parameters);
  }
}
