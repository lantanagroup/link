package com.lantanagroup.link.db;

import com.lantanagroup.link.TenantService;
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
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class MongoService {
  private static final Logger logger = LoggerFactory.getLogger(MongoService.class);
  public static final String AUDIT_COLLECTION = "audit";
  public static final String MEASURE_DEF_COLLECTION = "measureDef";
  public static final String USER_COLLECTION = "user";
  public static final String TENANT_CONFIG_COLLECTION = "tenantConfig";

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

  public MongoCollection<TenantConfig> getTenantConfigCollection() {
    return this.getDatabase().getCollection(TENANT_CONFIG_COLLECTION, TenantConfig.class);
  }

  public MongoCollection<MeasureDefinition> getMeasureDefinitionCollection() {
    return this.getDatabase().getCollection(MEASURE_DEF_COLLECTION, MeasureDefinition.class);
  }

  public MongoCollection<User> getUserCollection() {
    return this.getDatabase().getCollection(USER_COLLECTION, User.class);
  }

  public MeasureDefinition findMeasureDefinition(String measureId) {
    return this.getMeasureDefinitionCollection().find(eq("measureId", measureId)).first();
  }

  public MongoCollection<Audit> getAuditCollection() {
    return this.getDatabase().getCollection(AUDIT_COLLECTION, Audit.class);
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

  public TenantConfig getTenantConfig(String id) {
    return this.getTenantConfigCollection()
            .find(eq("_id", id))
            .first();
  }

  public List<TenantConfig> getTenantSchedules() {
    List<TenantConfig> tenantConfigs = new ArrayList<>();
    this.getTenantConfigCollection()
            .find()
            .projection(include("_id", "scheduling"))
            .into(tenantConfigs);
    return tenantConfigs;
  }

  public List<TenantConfig> searchTenantConfigs() {
    List<TenantConfig> tenantConfigs = new ArrayList<>();
    this.getTenantConfigCollection()
            .find()
            .projection(include("_id", "name", "description"))
            .into(tenantConfigs);
    return tenantConfigs;
  }

  public long deleteTenantConfig(String tenantId) {
    DeleteResult result = this.getTenantConfigCollection().deleteOne(eq("_id", tenantId));
    return result.getDeletedCount();
  }

  public void saveTenantConfig(TenantConfig tenantConfig) {
    Bson criteria = eq("_id", tenantConfig.getId());
    BasicDBObject update = new BasicDBObject();
    update.put("$set", tenantConfig);
    this.getTenantConfigCollection().updateOne(criteria, update, new UpdateOptions().upsert(true));
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

  public void audit(LinkCredentials credentials, HttpServletRequest request, TenantService tenantService, AuditTypes type, String notes) {
    Audit audit = new Audit();

    if (tenantService != null && tenantService.getConfig() != null) {
      audit.setTenantId(tenantService.getConfig().getId());
    }

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
