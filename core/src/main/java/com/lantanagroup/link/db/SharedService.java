package com.lantanagroup.link.db;

import com.lantanagroup.link.Hasher;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.MongoConfig;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
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

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class SharedService {
  private static final Logger logger = LoggerFactory.getLogger(SharedService.class);
  public static final String AUDIT_COLLECTION = "audit";
  public static final String MEASURE_DEF_COLLECTION = "measureDef";
  public static final String MEASURE_PACKAGE_COLLECTION = "measurePackage";
  public static final String USER_COLLECTION = "user";
  public static final String TENANT_CONFIG_COLLECTION = "tenantConfig";
  public static final String BULK_DATA_COLLECTION = "bulkDataStatus";
  public static final String DEFAULT_PASS = "linkt3mppass";
  public static final String DEFAULT_EMAIL = "default@nhsnlink.org";

  private MongoClient client;
  private MongoDatabase database;

  @Autowired
  private MongoConfig config;

  public MongoDatabase getDatabase() {
    if (this.database == null) {
      logger.info("Using database {}", this.config.getDatabase());
      this.database = getClient().getDatabase(this.config.getDatabase());

      // If no users in the db have a password
      if (this.database.getCollection(USER_COLLECTION).find(exists("password", true)).first() == null) {
        logger.warn("Did not find any users with passwords, ensuring at least one user has a password");

        // Find the default user by email
        User foundDefault = this.database.getCollection(USER_COLLECTION, User.class).find(eq("email", DEFAULT_EMAIL)).first();

        if (foundDefault == null) {
          logger.warn("Did not found a default user, creating a new default user with {}", DEFAULT_EMAIL);

          foundDefault = new User();
          foundDefault.setEmail(DEFAULT_EMAIL);
        }

        try {
          // Just set the password of the already-existing default user to the hash of the default password
          String salt = Hasher.getRandomSalt();
          foundDefault.setPasswordSalt(salt);
          foundDefault.setPasswordHash(Hasher.hash(DEFAULT_PASS, salt));
        } catch (Exception ex) {
          logger.error("Error hashing new/default user's password", ex);
          return this.database;
        }

        this.database.getCollection(USER_COLLECTION, User.class)
                .replaceOne(
                        eq("_id", foundDefault.getId()),
                        foundDefault,
                        new ReplaceOptions().upsert(true));
      }
    }

    return this.database;
  }

  public MongoCollection<BulkStatus> getBulkDataStatusCollection() {
    return this.getDatabase().getCollection(BULK_DATA_COLLECTION, BulkStatus.class);
  }
  public MongoCollection<Tenant> getTenantConfigCollection() {
    return this.getDatabase().getCollection(TENANT_CONFIG_COLLECTION, Tenant.class);
  }

  public MongoCollection<MeasureDefinition> getMeasureDefinitionCollection() {
    return this.getDatabase().getCollection(MEASURE_DEF_COLLECTION, MeasureDefinition.class);
  }

  public MongoCollection<MeasurePackage> getMeasurePackageCollection() {
    return this.getDatabase().getCollection(MEASURE_PACKAGE_COLLECTION, MeasurePackage.class);
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

  public Tenant getTenantConfig(String id) {
    return this.getTenantConfigCollection()
            .find(eq("_id", id))
            .first();
  }

  public List<Tenant> getTenantSchedules() {
    List<Tenant> tenants = new ArrayList<>();
    this.getTenantConfigCollection()
            .find()
            .projection(include("_id", "scheduling"))
            .into(tenants);
    return tenants;
  }

  public List<Tenant> getTenantFhirQueries() {
    List<Tenant> tenants = new ArrayList<>();
    this.getTenantConfigCollection()
            .find()
            .projection(include("_id", "fhirQuery"))
            .into(tenants);
    return tenants;
  }

  public List<Tenant> searchTenantConfigs() {
    List<Tenant> tenants = new ArrayList<>();
    this.getTenantConfigCollection()
            .find()
            .projection(include("_id", "name", "description", "database"))
            .into(tenants);
    return tenants;
  }

  public long deleteTenantConfig(String tenantId) {
    DeleteResult result = this.getTenantConfigCollection().deleteOne(eq("_id", tenantId));
    return result.getDeletedCount();
  }

  public void saveTenantConfig(Tenant tenant) {
    Bson criteria = eq("_id", tenant.getId());
    this.getTenantConfigCollection().replaceOne(criteria, tenant, new ReplaceOptions().upsert(true));
  }

  public List<MeasureDefinition> getAllMeasureDefinitions() {
    List<MeasureDefinition> measureDefinitions = new ArrayList<>();
    this.getMeasureDefinitionCollection()
            .find()
            .projection(include("measureId", "lastUpdated"))
            .into(measureDefinitions);
    return measureDefinitions;
  }

  public MeasureDefinition getMeasureDefinition(String measureId) {
    Bson criteria = eq("measureId", measureId);
    return this.getMeasureDefinitionCollection().find(criteria).first();
  }

  public void deleteMeasureDefinition(String measureId) {
    Bson criteria = eq("measureId", measureId);
    this.getMeasureDefinitionCollection().deleteOne(criteria);
  }

  public void saveMeasureDefinition(MeasureDefinition measureDefinition) {
    Bson criteria = eq("measureId", measureDefinition.getMeasureId());
    this.getMeasureDefinitionCollection().replaceOne(criteria, measureDefinition, new UpdateOptions().upsert(true));
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
    this.getAuditCollection().replaceOne(criteria, audit, new ReplaceOptions().upsert(true));
  }

  public void saveUser(User user) {
    Bson criteria = eq("_id", user.getId());
    this.getUserCollection().replaceOne(criteria, user, new ReplaceOptions().upsert(true));
  }

  public User getUser(String id) {
    Bson criteria = eq("_id", id);
    return this.getUserCollection().find(criteria).first();
  }

  public List<User> searchUsers(boolean includeDisabled) {
    List<User> users = new ArrayList<>();
    Bson criteria = exists("_id");

    if (!includeDisabled) {
      criteria = or(eq("enabled", true), not(exists("enabled")));
    }

    this.getUserCollection()
            .find(criteria)
            .map(u -> {
              u.setPasswordSalt(null);
              u.setPasswordHash(null);
              return u;
            })
            .into(users);
    return users;
  }

  public User findUser(String email) {
    Bson criteria = eq("email", email);
    return this.getUserCollection().find(criteria).first();
  }

  public List<MeasurePackage> getAllMeasurePackages() {
    List<MeasurePackage> measurePackages = new ArrayList<>();
    this.getMeasurePackageCollection()
            .find()
            .into(measurePackages);
    return measurePackages;
  }

  public void saveMeasurePackage(MeasurePackage measurePackage) {
    Bson criteria = eq("_id", measurePackage.getId());
    this.getMeasurePackageCollection().replaceOne(criteria, measurePackage, new UpdateOptions().upsert(true));
  }

  public void deleteMeasurePackage(String id) {
    Bson criteria = eq("_id", id);
    this.getMeasurePackageCollection().deleteOne(criteria);
  }
}
