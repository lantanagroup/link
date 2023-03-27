package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.MeasureDefinition;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.Report;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class MongoService {
  public static final String PATIENT_LIST_COLLECTION = "patientList";
  public static final String PATIENT_DATA_COLLECTION = "patientData";
  public static final String MEASURE_DEF_COLLECTION = "measureDef";
  public static final String REPORT_COLLECTION = "report";

  private MongoClient client;
  private MongoDatabase database;

  public MongoClient getClient() {
    if (this.client == null) {
      CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
      CodecRegistry codecRegistry = fromRegistries(
              fromCodecs(new BundleCodec(), new BaseCodec()),
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

  public MongoCollection<Report> getReportCollection() {
    return this.getDatabase().getCollection(MEASURE_DEF_COLLECTION, Report.class);
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

  public void savePatientData(PatientData patientData) {
    Bson criteria = eq("patientId", patientData.getPatientId());
    BasicDBObject update = new BasicDBObject();
    update.put("$set", patientData);
    this.getPatientDataCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
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
}
