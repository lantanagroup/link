package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.repositories.*;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TenantService {
  private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

  @Getter
  private Tenant config;

  private final ConceptMapRepository conceptMaps;
  private final PatientListRepository patientLists;
  private final ReportRepository reports;
  private final PatientDataRepository patientDatas;
  private final PatientMeasureReportRepository patientMeasureReports;
  private final AggregateRepository aggregates;
  private final DataSource dataSource;

  protected TenantService(Tenant config) {
    this.config = config;
    this.dataSource = DataSourceBuilder.create()
            .type(SQLServerDataSource.class)
            .url(config.getConnectionString())
            .build();
    this.conceptMaps = new ConceptMapRepository(this.dataSource);
    this.patientLists = new PatientListRepository(this.dataSource);
    this.reports = new ReportRepository(this.dataSource);
    this.patientDatas = new PatientDataRepository(this.dataSource);
    this.patientMeasureReports = new PatientMeasureReportRepository(this.dataSource);
    this.aggregates = new AggregateRepository(this.dataSource);
  }

  public static TenantService create(Tenant tenant) {
    return new TenantService(tenant);
  }

  public void testConnection() throws SQLException {
    this.dataSource.getConnection().getMetaData();
  }

  public void initDatabase() {
    logger.info("Initializing database for tenant {}", this.getConfig().getId());

    URL resource = this.getClass().getClassLoader().getResource("tenant-db.sql");

    if (resource == null) {
      logger.warn("Could not find tenant-db.sql file in class path");
      return;
    }

    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      String sql = Files.readString(Path.of(resource.toURI()));
      for (String stmtSql : sql.split("GO")) {
        try {
          Statement stmt = conn.createStatement();
          stmt.execute(stmtSql);
        } catch (SQLException e) {
          logger.error("Failed to execute statement for tenant {}", this.config.getId(), e);
        }
      }
    } catch (SQLServerException e) {
      logger.error("Failed to connect to tenant {} database: {}", this.config.getId(), e.getMessage());
    } catch (SQLException | NullPointerException e) {
      logger.error("Failed to initialize tenant {} database", this.config.getId(), e);
    } catch (IOException | URISyntaxException e) {
      logger.error("Could not read tenant-db.sql file for tenant {}", this.config.getId(), e);
    }
  }

  public static TenantService create(SharedService sharedService, String tenantId) {
    if (StringUtils.isEmpty(tenantId)) {
      return null;
    }

    Tenant tenant = sharedService.getTenantConfig(tenantId);

    if (tenant == null) {
      logger.error("Tenant {} not found", tenantId);
      return null;
    }

    return new TenantService(tenant);
  }

  public List<PatientList> getPatientLists(String reportId) {
    return this.patientLists.findByReportId(reportId);
  }

  public List<PatientList> getAllPatientLists() {
    return this.patientLists.findAll();
  }

  public int deletePatientListsLastUpdatedBefore(Date date) {
    return this.patientLists.deleteByLastUpdatedBefore(date);
  }

  public PatientList getPatientList(UUID id) {
    return this.patientLists.findById(id);
  }

  public PatientList findPatientList(String measureId, String periodStart, String periodEnd) {
    return this.patientLists.findByMeasureIdAndReportingPeriod(measureId, periodStart, periodEnd);
  }

  public void savePatientList(PatientList patientList) {
    this.patientLists.save(patientList);
  }

  public List<PatientData> findPatientData(String patientId) {
    return this.patientDatas.findByPatientId(patientId);
  }

  public int deletePatientDataRetrievedBefore(Date date) {
    return this.patientDatas.deleteByRetrievedBefore(date);
  }

  public void savePatientData(List<PatientData> patientData) {
    this.patientDatas.saveAll(patientData);
  }

  public Report getReport(String id) {
    return this.reports.findById(id);
  }

  public List<Report> searchReports() {
    return this.reports.findAll();
  }

  public void saveReport(Report report) {
    this.reports.save(report);
  }

  public void saveReport(Report report, List<PatientList> patientLists) {
    this.reports.save(report, patientLists);
  }

  public PatientMeasureReport getPatientMeasureReport(String id) {
    return this.patientMeasureReports.findById(id);
  }

  public List<PatientMeasureReport> getPatientMeasureReports(String reportId) {
    return this.patientMeasureReports.findByReportId(reportId);
  }

  public List<PatientMeasureReport> getPatientMeasureReports(String reportId, String measureId) {
    return this.patientMeasureReports.findByReportIdAndMeasureId(reportId, measureId);
  }

  public void savePatientMeasureReport(PatientMeasureReport patientMeasureReport) {
    this.patientMeasureReports.save(patientMeasureReport);
  }

  public List<Aggregate> getAggregates(String reportId) {
    return this.aggregates.findByReportId(reportId);
  }

  public void saveAggregate(Aggregate aggregate) {
    this.aggregates.save(aggregate);
  }

  public ConceptMap getConceptMap(String id) {
    return this.conceptMaps.findById(id);
  }

  public List<ConceptMap> searchConceptMaps() {
    List<ConceptMap> conceptMaps = this.conceptMaps.findAll();
    for (ConceptMap conceptMap : conceptMaps) {
      conceptMap.setConceptMap(null);
    }
    return conceptMaps;
  }

  public List<ConceptMap> getAllConceptMaps() {
    return this.conceptMaps.findAll();
  }

  public void saveConceptMap(ConceptMap conceptMap) {
    this.conceptMaps.save(conceptMap);
  }
}
