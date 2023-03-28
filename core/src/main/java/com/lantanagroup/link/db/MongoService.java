package com.lantanagroup.link.db;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.model.*;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
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
  public static final String AUDIT_COLLECTION = "audit";

  public static final String PATIENT_LIST_COLLECTION = "patientList";
  public static final String PATIENT_DATA_COLLECTION = "patientData";
  public static final String MEASURE_DEF_COLLECTION = "measureDef";
  public static final String REPORT_COLLECTION = "report";
  public static final String USER_COLLECTION = "user";
  public static final String PATIENT_MEASURE_REPORT_COLLECTION = "patientMeasureReport";
  private static final Logger logger = LoggerFactory.getLogger(MongoService.class);

  private MongoClient client;
  private MongoDatabase database;
  private static final String NAME = "name";

  public MongoDatabase getDatabase() {
    if (this.database == null) {
      this.database = getClient().getDatabase("link");
    }
    return this.database;
  }

  public MongoCollection<PatientList> getCensusCollection() {
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

  private static final String SUBJECT = "sub";

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
                      new BaseCodec<>(Bundle.class),
                      new BaseCodec<>(MeasureReport.class),
                      new BaseCodec<>(IBaseResource.class)),
              MongoClientSettings.getDefaultCodecRegistry(),
              pojoCodecRegistry);
      MongoClientSettings clientSettings = MongoClientSettings.builder()
              .applyConnectionString(new ConnectionString("mongodb://root:Temp123@localhost:27017"))
              .codecRegistry(codecRegistry)
              .build();
      this.client = MongoClients.create(clientSettings);
    }
    return this.client;
  }

  public PatientList findPatientList(String periodStart, String periodEnd, String measureId) {
    Bson criteria = and(eq("periodStart", periodStart), eq("periodEnd", periodEnd), eq("measureId", measureId));
    return this.getCensusCollection().find(criteria).first();
  }

  public void savePatientList(PatientList patientList) {
    Bson criteria = and(eq("periodStart", patientList.getPeriodStart()), eq("periodEnd", patientList.getPeriodEnd()), eq("measureId", patientList.getMeasureId()));
    BasicDBObject update = new BasicDBObject();
    update.put("$set", patientList);
    this.getCensusCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public FindIterable<PatientData> findPatientData(String patientId) {
    return this.getPatientDataCollection().find(eq("patientId", patientId));
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

  public FindIterable<PatientMeasureReport> getPatientMeasureReports(List<String> ids) {
    Bson criteria = in("_id", ids);
    return this.getPatientMeasureReportCollection().find(criteria);
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
}
