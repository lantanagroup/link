package com.lantanagroup.link.db;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lantanagroup.link.auth.LinkCredentials;
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
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class MongoService {
  public static final String AUDIT_COLLECTION = "audit";

  public static final String PATIENT_LIST_COLLECTION = "patientList";
  public static final String PATIENT_DATA_COLLECTION = "patientData";
  public static final String MEASURE_DEF_COLLECTION = "measureDef";
  public static final String REPORT_COLLECTION = "report";
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

  public PatientData findPatientData(String patientId) {
    return this.getPatientDataCollection().find(eq("patientId", patientId)).first();
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

  public Report findReport(List<String> measureIds, String periodStart, String periodEnd) {
    Bson criteria = and(eq("periodStart", periodStart), eq("periodEnd", periodEnd), eq("measureId", measureIds));
    return this.getReportCollection().find(criteria).first();
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

  public void savePatientData(PatientData patientData) {
    Bson criteria = and(
            eq("patientId", patientData.getPatientId()),
            eq("resourceType", patientData.getResourceType()),
            eq("resourceId", patientData.getResourceId()));
    BasicDBObject update = new BasicDBObject();
    update.put("$set", patientData);
    this.getPatientDataCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public void savePatientData(List<PatientData> patientData) {
    List<WriteModel<PatientData>> writeOperations = patientData.stream().map(pd -> {
      Bson criteria = and(
              eq("patientId", pd.getPatientId()),
              eq("resourceType", pd.getResourceType()),
              eq("resourceId", pd.getResourceId()));
      BasicDBObject update = new BasicDBObject();
      update.put("$set", pd);
      return new UpdateOneModel<PatientData>(criteria, update, new UpdateOptions().upsert(true));
    }).collect(Collectors.toList());

    this.getPatientDataCollection().bulkWrite(writeOperations);
  }

  public PatientMeasureReport getPatientMeasureReport(String id) {
    return this.getPatientMeasureReportCollection().find(eq("_id", id)).first();
  }

  public void savePatientMeasureReport(PatientMeasureReport patientMeasureReport) {
    Bson criteria = eq("_id", patientMeasureReport.getId());
    BasicDBObject update = new BasicDBObject();
    update.put("$set", patientMeasureReport);
    this.getPatientMeasureReportCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
  }

  public void audit(LinkCredentials user, HttpServletRequest request, AuditTypes type, String notes) {
    Audit audit = new Audit();

    if (user != null && user.getPractitioner() != null) {
      String payload = user.getJwt().getPayload();
      byte[] decodedBytes = Base64.getDecoder().decode(payload);
      String decodedString = new String(decodedBytes);
      JsonObject jsonObject = JsonParser.parseString(decodedString).getAsJsonObject();

      if (jsonObject.has(NAME)) {
        audit.setName(jsonObject.get(NAME).toString());
      }
      if (jsonObject.has(SUBJECT)) {
        audit.setUserId(jsonObject.get(SUBJECT).toString());
      }
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
}
