package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.repositories.ConceptMapRepository;
import com.lantanagroup.link.db.repositories.PatientListRepository;
import com.lantanagroup.link.db.repositories.ReportRepository;
import com.lantanagroup.link.model.ReportBase;
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

  public static final String PATIENT_DATA_COLLECTION = "patientData";
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

  public MongoCollection<PatientData> getPatientDataCollection() {
    return this.database.getCollection(PATIENT_DATA_COLLECTION, PatientData.class);
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

  public void deletePatientLists(List<UUID> ids) {
    this.patientLists.deleteByIds(ids);
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
    List<PatientData> patientData = new ArrayList<>();
    this.getPatientDataCollection()
            .find(eq("patientId", patientId))
            .into(patientData);
    return patientData;
  }

  public List<PatientData> getAllPatientData() {
    List<PatientData> allPatientData = new ArrayList<>();
    this.getPatientDataCollection().find()
            .projection(include("_id", "retrieved"))
            .into(allPatientData);
    return allPatientData;
  }

  public void deletePatientData(List<String> ids) {
    List<Bson> matchIds = ids.stream().map(id -> eq("_id", id)).collect(Collectors.toList());
    Bson criteria = or(matchIds);
    this.getPatientDataCollection().deleteMany(criteria);
  }

  @Async
  public void savePatientData(List<PatientData> patientData) {
    List<WriteModel<PatientData>> writeOperations = patientData.stream().map(pd -> {
      Bson criteria = and(
              eq("patientId", pd.getPatientId()),
              eq("resourceType", pd.getResourceType()),
              eq("resourceId", pd.getResourceId()));
      BasicDBObject setOnInsert = new BasicDBObject();
      setOnInsert.put("_id", (new ObjectId()).toString());
      BasicDBObject update = new BasicDBObject();
      update.put("$set", pd);
      update.put("$setOnInsert", setOnInsert);
      return new UpdateOneModel<PatientData>(criteria, update, new UpdateOptions().upsert(true));
    }).collect(Collectors.toList());

    this.getPatientDataCollection().bulkWrite(writeOperations);
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
