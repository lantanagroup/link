package com.lantanagroup.link.db;

import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.mappers.ValidationResultMapper;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.db.repositories.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TenantService {
  private static final Logger logger = LoggerFactory.getLogger(TenantService.class);
  private static final Map<String, ComboPooledDataSource> dataSourcesByJdbcUrl = new ConcurrentHashMap<>();

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
  private final QueryRepository queries;
  private final DataTraceRepository dataTraces;
  private final ValidationRepository validations;
  private final ValidationCategoryRepository validationCategories;

  protected TenantService(Tenant config, ComboPooledDataSource dataSource) {
    this.config = config;
    this.dataSource = dataSource;
    PlatformTransactionManager txManager = new DataSourceTransactionManager(this.dataSource);
    this.conceptMaps = new ConceptMapRepository(this.dataSource, txManager);
    this.patientLists = new PatientListRepository(this.dataSource, txManager);
    this.reports = new ReportRepository(this.dataSource, txManager);
    this.patientDatas = new PatientDataRepository(this.dataSource, txManager);
    this.patientMeasureReports = new PatientMeasureReportRepository(this.dataSource, txManager);
    this.aggregates = new AggregateRepository(this.dataSource, txManager);
    this.bulkStatuses = new BulkStatusRepository(this.dataSource, txManager);
    this.bulkStatusResults = new BulkStatusResultRepository(this.dataSource, txManager);
    this.queries = new QueryRepository(this.dataSource, txManager);
    this.dataTraces = new DataTraceRepository(this.dataSource, txManager);
    this.validations = new ValidationRepository(this.dataSource, txManager);
    this.validationCategories = new ValidationCategoryRepository(this.dataSource, txManager);
  }

  public static TenantService create(Tenant tenant) {
    return new TenantService(tenant, dataSourcesByJdbcUrl.computeIfAbsent(tenant.getConnectionString(), jdbcUrl -> {
      ComboPooledDataSource dataSource = new ComboPooledDataSource();
      dataSource.setJdbcUrl(jdbcUrl);
      return dataSource;
    }));
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

    return TenantService.create(tenant);
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

  public void beginReport(String reportId) {
    this.aggregates.deleteByReportId(reportId);
    this.patientMeasureReports.deleteByReportId(reportId);
    this.patientDatas.beginReport(reportId);
    this.dataTraces.deleteByReportId(reportId);
  }

  public List<PatientData> findPatientData(String patientId) {
    return this.patientDatas.findByPatientId(patientId);
  }

  public List<PatientData> findPatientData(String reportId, String patientId) {
    return this.patientDatas.findByReportIdAndPatientId(reportId, patientId);
  }

  public int deletePatientDataRetrievedBefore(Date date) {
    int result = this.patientDatas.deleteByRetrievedBefore(date);
    this.dataTraces.deleteByRetrievedBefore(date);
    this.queries.deleteByRetrievedBefore(date);
    return result;
  }

  public void deletePatientListById(UUID id) {
    this.patientLists.deleteById(id);
  }

  public void deleteAllPatientData(){
    this.validationCategories.deleteAll();
    this.validations.deleteAll();

    this.aggregates.deleteAll();
    this.patientMeasureReports.deleteAll();

    this.patientDatas.deleteAll();
    this.dataTraces.deleteAll();
    this.queries.deleteAll();

    this.reports.deleteAll();
    this.patientLists.deleteAll();
  }

  public void deletePatientByListAndPatientId(String patientId, UUID listId) {
    PatientList patientList = this.getPatientList(listId);
    if (patientList == null) {
      return;
    }
    var filteredList = patientList.getPatients().stream().filter(x -> {
      String identifier = x.getIdentifier();
      if (identifier != null && identifier.equals(patientId)) {
        return false;
      }
      String reference = x.getReference();
      if (reference != null) {
        String id = StringUtils.removeStartIgnoreCase(reference, "Patient/");
        return !id.equals(patientId);
      }
      return true;
    }).collect(Collectors.toList());
    patientList.setPatients(filteredList);
    patientList.setLastUpdated(new Date());
    this.savePatientList(patientList);
  }

  public void savePatientData(String reportId, List<PatientData> patientData) {
    this.patientDatas.saveAll(reportId, patientData);
  }

  public Report getReport(String id) {
    return this.reports.findById(id);
  }

  public List<Report> getReportsByPatientListId(UUID id) {
    return this.reports.findByPatientListId(id);
  }

  public Report findLastReport() {
    return this.reports.findLastReport();
  }

  public List<Report> searchReports() {
    return this.reports.findAll();
  }

  public String getReportInsights(String tenantId, String reportId, String version) {
    try (Connection connection = this.dataSource.getConnection()) {
      String sql = IOUtils.resourceToString("/insights-tenant.sql", StandardCharsets.UTF_8);
      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setNString(1, reportId);
      statement.execute();
      return SQLUtils.format(statement, "Patient lists", "Individual measure reports", "Aggregate measure reports",
              "Patient data", "Validation issues");
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveReport(Report report) {
    this.reports.save(report);
  }

  public void saveReport(Report report, List<PatientList> patientLists) {
    this.reports.save(report, patientLists);
  }

  public void deleteReport(String reportId){
    this.validations.deleteByReport(reportId);

    this.aggregates.deleteByReportId(reportId);
    this.patientMeasureReports.deleteByReportId(reportId);

    this.patientDatas.deleteByReportId(reportId);
    this.dataTraces.deleteByReportId(reportId);
    this.queries.deleteByReportId(reportId);

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
  public List<PatientMeasureReportSize> getPatientMeasureReportSize(String patientId, String reportId, String measureId, String patientMeasureReportId) {
    if(patientMeasureReportId != null && !patientMeasureReportId.isEmpty()) {
      var reportSize = patientMeasureReports.GetMeasureReportSizeById(patientMeasureReportId);
      return List.of(reportSize);
    }
    else {
        return patientMeasureReports.GetMeasureReportSize(patientId, reportId, measureId);
    }
  }

  public List<PatientDataSize> getPatientDataReportSize(String patientId, String resourceType, LocalDateTime startDate, LocalDateTime endDate) {
      return patientDatas.GetPatientDataResourceSizeInDateRange(patientId, resourceType, startDate, endDate);
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

  public void saveQuery(Query query) {
    try {
      this.queries.insert(query);
    } catch (Exception e) {
      logger.error("Failed to save query", e);
    }
  }

  public void saveDataTraces(UUID queryId, String patientId, List<IBaseResource> resources) {
    try {
      List<DataTrace> models = new ArrayList<>();
      for (IBaseResource resource : resources) {
        UUID dataTraceId = UUID.randomUUID();
        DataTrace.setId(resource, dataTraceId);
        DataTrace model = new DataTrace();
        model.setId(dataTraceId);
        model.setQueryId(queryId);
        model.setPatientId(patientId);
        model.setResourceType(resource.fhirType());
        model.setResourceId(resource.getIdElement().getIdPart());
        models.add(model);
      }
      this.dataTraces.insertAll(models);
    } catch (Exception e) {
      logger.error("Failed to save data traces", e);
    }
  }

  public void deleteValidationResults(String reportId) {
    this.validations.deleteByReport(reportId);
  }

  public void insertValidationResults(String reportId, List<ValidationResult> models) {
    this.validations.insertAll(reportId, models);
  }

  public List<ValidationResult> getValidationResults(String reportId) {
    return this.validations.findValidationResults(reportId, null, null);
  }

  public List<ValidationResult> getValidationResults(String reportId, OperationOutcome.IssueSeverity severity, String code) {
    return this.validations.findValidationResults(reportId, severity, code);
  }

  public OperationOutcome getValidationResultsOperationOutcome(String reportId, OperationOutcome.IssueSeverity severity, String code) {
    return ValidationResultMapper.toOperationOutcome(this.getValidationResults(reportId, severity, code));
  }

  public void insertValidationResultCategories(List<ValidationResultCategory> models) {
    this.validationCategories.insertAll(models);
  }

  public List<ValidationResultCategory> findValidationResultCategoriesByReport(String reportId) {
    return this.validationCategories.findByReportId(reportId);
  }

  public List<ValidationResult> findValidationResultsByCategory(String reportId, String categoryCode) {
    return this.validations.findByCategory(reportId, categoryCode);
  }

  public Integer countUncategorizedValidationResults(String reportId) {
    return this.validations.countUncategorized(reportId);
  }

  public List<ValidationResult> getUncategorizedValidationResults(String reportId) {
    return this.validations.getUncategorized(reportId);
  }

  public String getOrganizationID(){
    return this.config.getCdcOrgId();
  }
}
