package com.lantanagroup.link.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.Hasher;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.MongoConfig;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
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
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Projections.include;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class SharedService {
  private static final Logger logger = LoggerFactory.getLogger(SharedService.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  public static final String AUDIT_COLLECTION = "audit";
  public static final String USER_COLLECTION = "user";
  public static final String BULK_DATA_COLLECTION = "bulkDataStatus";
  public static final String DEFAULT_PASS = "linkt3mppass";
  public static final String DEFAULT_EMAIL = "default@nhsnlink.org";

  private MongoClient client;
  private MongoDatabase database;

  @Autowired
  private MongoConfig config;

  private Connection getSQLConnection() {
    try {
      Connection conn = DriverManager.getConnection(this.config.getSqlConnectionString());
      if (conn != null) {
        DatabaseMetaData dm = conn.getMetaData();
        return conn;
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    }

    return null;
  }

  public MongoDatabase getDatabase() {
    if (this.database == null) {
      logger.info("Using database {}", this.config.getDatabase());
      this.database = getClient().getDatabase(this.config.getDatabase());

      // If no users in the db have a password
      if (this.database.getCollection(USER_COLLECTION).find(exists("passwordHash", true)).first() == null) {
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
          byte[] salt = Hasher.getRandomSalt();
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

  public Tenant getTenantConfig(String tenantId)
  {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT json FROM [dbo].[tenantConfig] WHERE id = ?");
      ps.setNString(1, tenantId);
      ResultSet rs = ps.executeQuery();

      Tenant tenant = null;

      if(rs.next()) {
        var json = rs.getString(0);
        tenant = mapper.readValue(json, Tenant.class);
      }

      assert tenant != null;

      return tenant;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public ArrayList<Tenant> getTenantConfigs() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT json FROM [dbo].[tenantConfig]");

      ResultSet rs = ps.executeQuery();
      var tenants = new ArrayList<Tenant>();

      while(rs.next()) {
        var json = rs.getString(0);
        var tenant = mapper.readValue(json, Tenant.class);
        tenants.add(tenant);
      }

      return tenants;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public int deleteTenantConfig(String tenantId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("DELETE FROM [dbo].[tenantConfig] WHERE id = ?");
      ps.setNString(1, tenantId);

      var rowsAffected = ps.executeUpdate();

      assert rowsAffected <= 1;

      return rowsAffected;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveTenantConfig(Tenant tenant){
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveTenant (?, ?) }");
      cs.setNString("tenantId", tenant.getId());
      cs.setNString("json", mapper.writeValueAsString(tenant));

      cs.executeUpdate();

    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MeasureDefinition getMeasureDefinition(String measureId) {
    //return this.getDatabase().getCollection(MEASURE_DEF_COLLECTION, MeasureDefinition.class);

    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT bundle FROM [dbo].[measureDef] WHERE measureId = ?");
      ps.setNString(1, measureId);

      ResultSet rs = ps.executeQuery();

      MeasureDefinition measureDef = null;

      if(rs.next()) {
        var json = rs.getString(0);
        measureDef = mapper.readValue(json, MeasureDefinition.class);
      }

      assert measureDef != null;

      return measureDef;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public ArrayList<MeasureDefinition> getMeasureDefinitions() {
    //return this.getDatabase().getCollection(MEASURE_DEF_COLLECTION, MeasureDefinition.class);

    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT bundle FROM [dbo].[measureDef]");

      ResultSet rs = ps.executeQuery();
      var measureDefs = new ArrayList<MeasureDefinition>();

      while(rs.next()) {
        var json = rs.getString(0);
        var tenant = mapper.readValue(json, MeasureDefinition.class);
        measureDefs.add(tenant);
      }

      return measureDefs;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public long deleteMeasureDefinition(String measureId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("DELETE FROM [dbo].[measureDef] WHERE measureId = ?");
      ps.setNString(1, measureId);

      var rowsAffected = ps.executeUpdate();

      assert rowsAffected <= 1;

      return rowsAffected;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveMeasureDefinition(MeasureDefinition measureDefinition) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveMeasureDef (?, ?, ?) }");
      cs.setNString("measureId", measureDefinition.getMeasureId());
      cs.setNString("bundle", mapper.writeValueAsString(measureDefinition.getBundle()));
      cs.setString("lastUpdated", measureDefinition.getLastUpdated().toString());

      cs.executeUpdate();

    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MeasurePackage getMeasurePackage(String packageId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT measures FROM [dbo].[measurePackage] WHERE packageId = ?");
      ps.setNString(1, packageId);
      ResultSet rs = ps.executeQuery();

      MeasurePackage measurePackage = null;

      if(rs.next()) {
        var json = rs.getString(0);
        measurePackage = mapper.readValue(json, MeasurePackage.class);
      }

      assert measurePackage != null;

      return measurePackage;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public ArrayList<MeasurePackage> getMeasurePackages() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT measures FROM [dbo].[measurePackage]");

      ResultSet rs = ps.executeQuery();
      var packages = new ArrayList<MeasurePackage>();

      while(rs.next()) {
        var json = rs.getString(0);
        var measurePackage = mapper.readValue(json, MeasurePackage.class);
        packages.add(measurePackage);
      }

      return packages;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveMeasurePackage(MeasurePackage measurePackage) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveMeasurePackage (?, ?) }");
      cs.setNString("packageId", measurePackage.getId());
      cs.setNString("measures", mapper.writeValueAsString(measurePackage));

      cs.executeUpdate();

    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public long deleteMeasurePackage(String packageId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("DELETE FROM [dbo].[measurePackage] WHERE packageId = ?");
      ps.setNString(1, packageId);

      var rowsAffected = ps.executeUpdate();

      assert rowsAffected <= 1;

      return rowsAffected;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public ArrayList<Audit> getAudits() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [dbo].[audit]");

      ResultSet rs = ps.executeQuery();
      var audits = new ArrayList<Audit>();

      while(rs.next()) {
        var iD = rs.getString(0);
        var network = rs.getString(1);
        var notes = rs.getString(2);
        var tenantId = rs.getString(3);
        var timeStamp = rs.getDate(4);
        var type = rs.getString(5);
        var userId = rs.getString(6);

        var audit = new Audit();
        audit.setId(iD);
        audit.setNetwork(network);
        audit.setNotes(notes);
        audit.setTenantId(tenantId);
        audit.setTimestamp(timeStamp);
        audit.setType(AuditTypes.valueOf(type));
        audit.setUserId(userId);

        audits.add(audit);
      }

      return audits;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveAudit(Audit audit) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveAudit (?, ?, ?, ?, ?, ?, ?) }");
      cs.setNString("id", audit.getId());
      cs.setNString("network", audit.getNetwork());
      cs.setNString("notes", audit.getNotes());
      cs.setNString("tenantId", audit.getTenantId());
      cs.setDateTime("timeStamp", audit.getTimestamp().toString());
      cs.setNString("type", audit.getType().toString());
      cs.setString("userID", audit.getUserId());

      cs.executeUpdate();

    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
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
                      new BaseCodec<>(org.hl7.fhir.r4.model.ConceptMap.class)
              ),
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

    this.saveAudit(audit);
  }

  public void saveUser(User user) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveUser (?, ?, ?, ?, ?, ?) }");
      cs.setString("id", user.getId());
      cs.setNString("email", user.getEmail());
      cs.setNString("name", user.getName());
      cs.setBoolean("enabled", user.getEnabled());
      cs.setNString("passwordHash", user.getPasswordHash());
      cs.setBytes("passwordSalt", user.getPasswordSalt());

      try (ResultSet rs = cs.executeQuery()) {
        if (rs.next()) {
          user.setId(rs.getString(1));
        }
      }
    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public User getUser(String id) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [user] WHERE id = CONVERT(UNIQUEIDENTIFIER, ?)");
      ps.setObject(1, id);
      ResultSet rs = ps.executeQuery();

      if (!rs.next()) {
        return null;
      }

      return User.create(rs);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public List<User> searchUsers(boolean includeDisabled) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [user]");

      ResultSet rs = ps.executeQuery();
      List<User> users = new ArrayList<>();

      while (rs.next()) {
        User next = User.create(rs);
        next.setPasswordSalt(null);
        next.setPasswordHash(null);
        users.add(next);
      }

      return users.stream()
              .filter(u -> {
                if (includeDisabled) {
                  return true;
                }
                return u.getEnabled();
              }).collect(Collectors.toList());
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public User findUser(String email) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [user] WHERE enabled = 1 AND email = ?");
      ps.setString(1, email);
      ResultSet rs = ps.executeQuery();

      if (!rs.next()) {
        return null;
      }

      return User.create(rs);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }
}
