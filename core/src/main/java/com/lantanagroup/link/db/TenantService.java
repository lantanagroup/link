package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.repositories.ConceptMapRepository;
import com.lantanagroup.link.db.repositories.PatientDataRepository;
import com.lantanagroup.link.db.repositories.PatientListRepository;
import com.lantanagroup.link.db.repositories.ReportRepository;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.scheduling.annotation.Async;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;

public class TenantService {
  private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

  @Getter
  private Tenant config;

  private MongoDatabase database;

  private final ConceptMapRepository conceptMaps;
  private final PatientListRepository patientLists;
  private final ReportRepository reports;
  private final PatientDataRepository patientDatas;

  public static final String PATIENT_MEASURE_REPORT_COLLECTION = "patientMeasureReport";
  public static final String AGGREGATE_COLLECTION = "aggregate";

  protected TenantService(Tenant config) {
    this.config = config;
    DataSource dataSource = DataSourceBuilder.create()
            .type(SQLServerDataSource.class)
            .url(config.getConnectionString())
            .build();
    this.conceptMaps = new ConceptMapRepository(dataSource);
    this.patientLists = new PatientListRepository(dataSource);
    this.reports = new ReportRepository(dataSource);
    this.patientDatas = new PatientDataRepository(dataSource);
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

  public MongoCollection<PatientMeasureReport> getPatientMeasureReportCollection() {
    return this.database.getCollection(PATIENT_MEASURE_REPORT_COLLECTION, PatientMeasureReport.class);
  }

  public MongoCollection<Aggregate> getAggregateCollection() {
    return this.database.getCollection(AGGREGATE_COLLECTION, Aggregate.class);
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
    return this.getPatientMeasureReportCollection().find(eq("_id", id)).first();
  }

  public List<PatientMeasureReport> getPatientMeasureReports(List<String> ids) {
    List<PatientMeasureReport> patientMeasureReports = new ArrayList<>();
    Bson criteria = in("_id", ids);
    this.getPatientMeasureReportCollection()
            .find(criteria)
            .into(patientMeasureReports);
    return patientMeasureReports;
  }

  public void savePatientMeasureReport(PatientMeasureReport patientMeasureReport) {
    Bson criteria = eq("_id", patientMeasureReport.getId());
    this.getPatientMeasureReportCollection().replaceOne(criteria, patientMeasureReport, new ReplaceOptions().upsert(true));
  }

  public List<Aggregate> getAggregates(String reportId) {
    List<Bson> matchIds = List.of();
    Bson criteria = or(matchIds);
    List<Aggregate> aggregates = new ArrayList<>();
    this.getAggregateCollection()
            .find(criteria)
            .into(aggregates);
    return aggregates;
  }

  public void saveAggregate(Aggregate aggregate) {
    Bson criteria = eq("_id", aggregate.getId());
    this.getAggregateCollection().replaceOne(criteria, aggregate, new ReplaceOptions().upsert(true));
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
