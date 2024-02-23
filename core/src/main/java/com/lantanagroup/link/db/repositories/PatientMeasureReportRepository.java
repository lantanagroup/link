package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.StreamUtils;
import com.lantanagroup.link.db.mappers.PatientMeasureReportMapper;
import com.lantanagroup.link.db.mappers.PatientMeasureReportSizeMapper;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.db.model.PatientMeasureReportSize;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientMeasureReportRepository {
  private static final PatientMeasureReportMapper mapper = new PatientMeasureReportMapper();
  private static final PatientMeasureReportSizeMapper sizeMapper = new PatientMeasureReportSizeMapper();
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

  public PatientMeasureReportSize GetMeasureReportSizeById(String id) {
    String sql =  "SELECT pmr.patientId,pmr.reportId,pmr.measureID, CAST(SUM(DATALENGTH(pmr.measureReport)) as FLOAT)/1024.0 as sizeKb FROM dbo.patientMeasureReport AS pmr WHERE id = :id GROUP BY pmr.patientId, pmr.reportId, pmr.measureId";

    Map<String, ?> parameters = Map.of("id", id);

    return jdbc.query(sql, parameters, sizeMapper).stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElse(null);
  }

  public List<PatientMeasureReportSize> GetMeasureReportSize(String patientId, String reportId, String measureId) {
    String sql =  "SELECT pmr.patientId, pmr.reportId, pmr.measureID, CAST(SUM(DATALENGTH(pmr.measureReport)) as FLOAT)/1024.0 as sizeKb FROM dbo.patientMeasureReport AS pmr WHERE (ISNULL(:reportId, '') = '' OR reportId = :reportId) AND (ISNULL(:measureId, '') = '' OR measureId = :measureId) AND (ISNULL(:patientId, '') = ''  OR patientId = :patientId) GROUP BY pmr.patientId, pmr.reportId, pmr.measureId";

    if(reportId == null) reportId = "";
    if(measureId == null) measureId = "";
    if(patientId == null) patientId = "";

    HashMap<String, String> parameters = new HashMap<String, String>();
    parameters.put("reportId", reportId);
    parameters.put("patientId", patientId);
    parameters.put("measureId", measureId);

    return jdbc.query(sql, parameters, sizeMapper);
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

  public void deleteAll() {
    String sql = "DELETE FROM dbo.patientMeasureReport;";
    jdbc.update(sql, Map.of());
  }
}
