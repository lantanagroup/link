package com.lantanagroup.link;

import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;

public class TenantService {
  private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

  @Getter
  private Tenant config;

  private MongoDatabase database;

  public static final String PATIENT_LIST_COLLECTION = "patientList";
  public static final String PATIENT_DATA_COLLECTION = "patientData";
  public static final String REPORT_COLLECTION = "report";
  public static final String PATIENT_MEASURE_REPORT_COLLECTION = "patientMeasureReport";
  public static final String AGGREGATE_COLLECTION = "aggregate";
  public static final String CONCEPT_MAP_COLLECTION = "conceptMap";

  protected TenantService(MongoDatabase database, Tenant config) {
    this.database = database;
    this.config = config;
  }

  public static TenantService create(MongoService mongoService, String tenantId) {
    if (StringUtils.isEmpty(tenantId)) {
      return null;
    }

    Tenant tenant = mongoService.getTenantConfig(tenantId);

    if (tenant == null) {
      logger.error("Tenant {} not found", tenantId);
      return null;
    }

    return new TenantService(mongoService.getClient().getDatabase(tenant.getDatabase()), tenant);
  }

  public MongoCollection<PatientList> getPatientListCollection() {
    return this.database.getCollection(PATIENT_LIST_COLLECTION, PatientList.class);
  }

  public MongoCollection<PatientData> getPatientDataCollection() {
    return this.database.getCollection(PATIENT_DATA_COLLECTION, PatientData.class);
  }

  public MongoCollection<Report> getReportCollection() {
    return this.database.getCollection(REPORT_COLLECTION, Report.class);
  }

  public MongoCollection<PatientMeasureReport> getPatientMeasureReportCollection() {
    return this.database.getCollection(PATIENT_MEASURE_REPORT_COLLECTION, PatientMeasureReport.class);
  }

  public MongoCollection<Aggregate> getAggregateCollection() {
    return this.database.getCollection(AGGREGATE_COLLECTION, Aggregate.class);
  }

  public MongoCollection<ConceptMap> getConceptMapCollection() {
    return this.database.getCollection(CONCEPT_MAP_COLLECTION, ConceptMap.class);
  }

  public List<PatientList> getPatientLists(List<String> ids) {
    List<Bson> matchIds = ids.stream().map(id -> eq("_id", id)).collect(Collectors.toList());
    Bson criteria = or(matchIds);
    List<PatientList> patientLists = new ArrayList<>();
    this.getPatientListCollection()
            .find(criteria)
            .into(patientLists);
    return patientLists;
  }

  public List<PatientList> getAllPatientLists() {
    List<PatientList> patientLists = new ArrayList<>();
    this.getPatientListCollection()
            .find()
            .projection(include("measureId", "_id", "periodStart", "periodEnd"))
            .into(patientLists);
    return patientLists;
  }

  public PatientList getPatientList(String id) {
    return this.getPatientListCollection()
            .find(eq("_id", id))
            .first();
  }

  public PatientList findPatientList(String periodStart, String periodEnd, String measureId) {
    Bson criteria = and(eq("periodStart", periodStart), eq("periodEnd", periodEnd), eq("measureId", measureId));
    return this.getPatientListCollection().find(criteria).first();
  }

  public void savePatientList(PatientList patientList) {
    Bson criteria = and(eq("periodStart", patientList.getPeriodStart()), eq("periodEnd", patientList.getPeriodEnd()), eq("measureId", patientList.getMeasureId()));
    this.getPatientListCollection().replaceOne(criteria, patientList, new ReplaceOptions().upsert(true));
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
    return this.getReportCollection().find(eq("_id", id)).first();
  }

  public void saveReport(Report report) {
    Bson criteria = and(eq("periodStart", report.getPeriodStart()), eq("periodEnd", report.getPeriodEnd()), eq("measureIds", report.getMeasureIds()));
    this.getReportCollection().replaceOne(criteria, report, new ReplaceOptions().upsert(true));
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

  public List<Aggregate> getAggregates(List<String> ids) {
    List<Bson> matchIds = ids.stream().map(id -> eq("_id", id)).collect(Collectors.toList());
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
    Bson criteria = eq("_id", id);
    return this.getConceptMapCollection().find(criteria).first();
  }

  public List<ConceptMap> getAllConceptMaps() {
    List<ConceptMap> conceptMaps = new ArrayList<>();

    // resourceType is needed in the projection for HAPI to deserialize it
    this.getConceptMapCollection()
            .find()
            .projection(include("_id", "resource.name", "resource.resourceType", "resource.id"))
            .into(conceptMaps);
    return conceptMaps;
  }

  public void saveConceptMap(ConceptMap conceptMap) {
    Bson criteria = eq("_id", conceptMap.getId());
    this.getConceptMapCollection().replaceOne(criteria, conceptMap, new ReplaceOptions().upsert(true));
  }
}
