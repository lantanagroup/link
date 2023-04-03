package com.lantanagroup.link.db;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.MongoConfig;
import com.lantanagroup.link.db.model.*;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class MongoService {
  private static final Logger logger = LoggerFactory.getLogger(MongoService.class);
  public static final String AUDIT_COLLECTION = "audit";
  public static final String PATIENT_LIST_COLLECTION = "patientList";
  public static final String PATIENT_DATA_COLLECTION = "patientData";
  public static final String MEASURE_DEF_COLLECTION = "measureDef";
  public static final String REPORT_COLLECTION = "report";
  public static final String USER_COLLECTION = "user";
  public static final String PATIENT_MEASURE_REPORT_COLLECTION = "patientMeasureReport";
  public static final String CONCEPT_MAP_COLLECTION = "conceptMap";

  private MongoClient client;
  private MongoDatabase database;

  @Autowired
  private MongoConfig config;

  public MongoDatabase getDatabase() {
    if (this.database == null) {
      logger.info("Using database {}", this.config.getDatabase());
      this.database = getClient().getDatabase(this.config.getDatabase());
    }
    return this.database;
  }

  public MongoCollection<PatientList> getPatientListCollection() {
    return this.getDatabase().getCollection(PATIENT_LIST_COLLECTION, PatientList.class);
  }

  public MongoCollection<PatientData> getPatientDataCollection() {
    return this.getDatabase().getCollection(PATIENT_DATA_COLLECTION, PatientData.class);
  }

  public MongoCollection<MeasureDefinition> getMeasureDefinitionCollection() {
    return this.getDatabase().getCollection(MEASURE_DEF_COLLECTION, MeasureDefinition.class);
  }

  public MongoCollection<User> getUserCollection() {
    return this.getDatabase().getCollection(USER_COLLECTION, User.class);
  }

  public MongoCollection<ConceptMap> getConceptMapCollection() {
    return this.getDatabase().getCollection(CONCEPT_MAP_COLLECTION, ConceptMap.class);
  }

  public static String getRemoteAddress(HttpServletRequest request) {
    String remoteAddress;
    if (request.getHeader("X-FORWARED-FOR") != null) {
      logger.debug("X-FORWARED-FOR IP is: " + request.getHeader("X-FORWARED-FOR"));
    }

    if (request.getHeader("X-REAL-IP") != null) {
      logger.debug("X-REAL-IP is: " + request.getHeader("X-REAL-IP") + " and is being used for remoteAddress");
      remoteAddress = request.getHeader("X-REAL-IP");
    } else {
      logger.debug("X-REAL-IP IP is not found.");
      remoteAddress = request.getRemoteAddr() != null ? (request.getRemoteHost() != null ? request.getRemoteAddr() + "(" + request.getRemoteHost() + ")" : request.getRemoteAddr()) : "";
    }
    return remoteAddress;
  }

  public MongoClient getClient() {
    if (this.client == null) {
      CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
      CodecRegistry codecRegistry = fromRegistries(
              fromCodecs(
                      new BaseCodec<>(IBaseResource.class),
                      new BaseCodec<>(Bundle.class),
                      new BaseCodec<>(MeasureReport.class),
                      new BaseCodec<>(org.hl7.fhir.r4.model.ConceptMap.class)),
              MongoClientSettings.getDefaultCodecRegistry(),
              pojoCodecRegistry);

      logger.info("Connecting to mongo database with connection string {}", this.config.getConnectionString());

      MongoClientSettings clientSettings = MongoClientSettings.builder()
              .applyConnectionString(new ConnectionString(this.config.getConnectionString()))
              .codecRegistry(codecRegistry)
              .build();
      this.client = MongoClients.create(clientSettings);
    }

    return this.client;
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
    BasicDBObject update = new BasicDBObject();
    update.put("$set", patientList);
    this.getPatientListCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
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

  public MongoCollection<Report> getReportCollection() {
    return this.getDatabase().getCollection(REPORT_COLLECTION, Report.class);
  }

  public MongoCollection<PatientMeasureReport> getPatientMeasureReportCollection() {
    return this.getDatabase().getCollection(PATIENT_MEASURE_REPORT_COLLECTION, PatientMeasureReport.class);
  }

  public MeasureDefinition findMeasureDefinition(String measureId) {
    return this.getMeasureDefinitionCollection().find(eq("measureId", measureId)).first();
  }

  public List<MeasureDefinition> getAllMeasureDefinitions() {
    List<MeasureDefinition> measureDefinitions = new ArrayList<>();
    this.getMeasureDefinitionCollection()
            .find()
            .projection(include("measureId", "lastUpdated"))
            .into(measureDefinitions);
    return measureDefinitions;
  }

  public void saveMeasureDefinition(MeasureDefinition measureDefinition) {
    Bson criteria = eq("measureId", measureDefinition.getMeasureId());
    BasicDBObject update = new BasicDBObject();
    update.put("$set", measureDefinition);
    this.getMeasureDefinitionCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public Report findReport(String id) {
    return this.getReportCollection().find(eq("_id", id)).first();
  }

  public void saveReport(Report report) {
    Bson criteria = and(eq("periodStart", report.getPeriodStart()), eq("periodEnd", report.getPeriodEnd()), eq("measureIds", report.getMeasureIds()));
    BasicDBObject update = new BasicDBObject();
    update.put("$set", report);
    this.getReportCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public MongoCollection<Audit> getAuditCollection() {
    return this.getDatabase().getCollection(AUDIT_COLLECTION, Audit.class);
  }

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
    BasicDBObject update = new BasicDBObject();
    update.put("$set", patientMeasureReport);
    this.getPatientMeasureReportCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public void audit(LinkCredentials credentials, HttpServletRequest request, AuditTypes type, String notes) {
    Audit audit = new Audit();

    if (credentials != null && credentials.getUser() != null) {
      audit.setUserId(credentials.getUser().getId());

      if (StringUtils.isNotEmpty(credentials.getUser().getName())) {
        audit.setName(credentials.getUser().getName());
      }
    } else {
      audit.setName("Link System");
    }

    if (request != null) {
      String remoteAddress;
      remoteAddress = getRemoteAddress(request);

      if (remoteAddress != null) {
        audit.setNetwork(remoteAddress);
      }
    }

    audit.setType(type);
    audit.setTimestamp(new Date());
    audit.setNotes(notes);

    Bson criteria = eq("_id", audit.getId());
    BasicDBObject update = new BasicDBObject();
    update.put("$set", audit);
    this.getAuditCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public void saveUser(User user) {
    Bson criteria = eq("_id", user.getId());
    BasicDBObject update = new BasicDBObject();
    update.put("$set", user);
    this.getUserCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public User getUser(String id) {
    Bson criteria = eq("_id", id);
    return this.getUserCollection().find(criteria).first();
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
    BasicDBObject update = new BasicDBObject();
    update.put("$set", conceptMap);
    this.getConceptMapCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }
}
