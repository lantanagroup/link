package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.repositories.*;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TenantService {
  private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

  @Getter
  private final Tenant config;

  private final DataSource dataSource;
  private final ConceptMapRepository conceptMaps;
  private final PatientListRepository patientLists;
  private final ReportRepository reports;
  private final PatientDataRepository patientDatas;
  private final PatientMeasureReportRepository patientMeasureReports;
  private final AggregateRepository aggregates;
  private final BulkStatusRepository bulkStatuses;
  private final BulkStatusResultRepository bulkStatusResults;

  protected TenantService(Tenant config) {
    this.config = config;
    this.dataSource = DataSourceBuilder.create()
            .type(SQLServerDataSource.class)
            .url(config.getConnectionString())
            .build();
    PlatformTransactionManager txManager = new DataSourceTransactionManager(this.dataSource);
    this.conceptMaps = new ConceptMapRepository(this.dataSource, txManager);
    this.patientLists = new PatientListRepository(this.dataSource, txManager);
    this.reports = new ReportRepository(this.dataSource, txManager);
    this.patientDatas = new PatientDataRepository(this.dataSource, txManager);
    this.patientMeasureReports = new PatientMeasureReportRepository(this.dataSource, txManager);
    this.aggregates = new AggregateRepository(this.dataSource, txManager);
    this.bulkStatuses = new BulkStatusRepository(this.dataSource, txManager);
    this.bulkStatusResults = new BulkStatusResultRepository(this.dataSource, txManager);
  }

  public static TenantService create(Tenant tenant) {
    return new TenantService(tenant);
  }

  public void testConnection() throws SQLException {
    this.dataSource.getConnection().getMetaData();
  }

  public void initDatabase() {
    logger.info("Initializing tenant database: {}", this.getConfig().getId());
    try (Connection connection = this.dataSource.getConnection()) {
      SQLScriptExecutor.execute(connection, new ClassPathResource("tenant-db.sql"));
    } catch (Exception e) {
      logger.error("Failed to initialize tenant database", e);
      throw new RuntimeException(e);
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

  public void deletePatientListById(UUID id) { this.patientLists.deleteById(id);}

  public void deleteAllPatientData(){
    this.patientLists.deleteAll();
    this.patientDatas.deleteAll();
  }

  public void deletePatientByListAndPatientId(String patientId, UUID listId) {
    PatientList patientList = this.getPatientList(listId);
    var filteredList = patientList.getPatients().stream().filter(x -> !x.getIdentifier().equals(patientId)).collect(Collectors.toList());
    patientList.setPatients(filteredList);
    this.patientDatas.deleteByPatientId(patientId);
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

  public void deleteReport(String reportId){
    this.reports.deleteById(reportId);
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

  public List<BulkStatus> getBulkStatuses() {
    return this.bulkStatuses.findAll();
  }

  public BulkStatus getBulkStatusById(UUID id) {
    return this.bulkStatuses.findById(id);
  }

  public List<BulkStatus> getBulkPendingStatusesWithPopulatedUrl() {
    return this.bulkStatuses.findPendingWithUrl();
  }

  public void saveBulkStatus(BulkStatus bulkStatus) {
    this.bulkStatuses.save(bulkStatus);
  }

  public List<BulkStatusResult> getBulkStatusResults() {
    return this.bulkStatusResults.findAll();
  }

  public void saveBulkStatusResult(BulkStatusResult bulkStatusResult) {
    this.bulkStatusResults.save(bulkStatusResult);
  }
}
