package com.lantanagroup.link.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.model.LogMessage;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SharedService {
  private static final Logger logger = LoggerFactory.getLogger(SharedService.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private ApiConfig config;

  static {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public Connection getSQLConnection() {
    try {
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      Connection conn = DriverManager.getConnection(this.config.getConnectionString());
      if (conn != null) {
        DatabaseMetaData dm = conn.getMetaData();
        return conn;
      }
    } catch (SQLException ex) {
      logger.error("Could not establish connection to database", ex);
    } catch (ClassNotFoundException ex) {
      logger.error("Could not load driver for SQL server database", ex);
    }

    return null;
  }

  public void initDatabase() {
    logger.info("Initializing shared database");

    URL resource = this.getClass().getClassLoader().getResource("shared-db.sql");

    if (resource == null) {
      logger.warn("Could not find shared-db.sql file in class path");
      return;
    }

    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      String sql = Helper.readInputStream(resource.openStream());
      for (String stmtSql : sql.split("(?i)(?:^|\\R)\\s*GO\\s*(?:\\R|$)")) {
        try {
          Statement stmt = conn.createStatement();
          stmt.execute(stmtSql);
        } catch (SQLException e) {
          logger.error("Failed to execute statement to initialize shared db", e);
          return;
        }
      }
    } catch (SQLException | NullPointerException e) {
      logger.error("Failed to connect to shared database", e);
    } catch (IOException e) {
      logger.error("Could not read shared-db.sql file", e);
    }
  }

  public Tenant getTenantConfig(String tenantId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT json FROM [dbo].[tenantConfig] WHERE id = ?");
      ps.setNString(1, tenantId);
      ResultSet rs = ps.executeQuery();

      Tenant tenant = null;

      if(rs.next()) {
        var json = rs.getString(1);
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

  public List<String> getTenantConnectionStrings(String excludeTenantId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps;

      if (StringUtils.isNotEmpty(excludeTenantId)) {
        ps = conn.prepareStatement("SELECT JSON_VALUE([json], '$.connectionString') AS connectionString FROM [dbo].[tenantConfig] WHERE JSON_VALUE([json], '$.id') != ?");
        ps.setNString(1, excludeTenantId);
      } else {
        ps = conn.prepareStatement("SELECT JSON_VALUE([json], '$.connectionString') AS connectionString FROM [dbo].[tenantConfig]");
      }

      ResultSet rs = ps.executeQuery();
      List<String> connectionStrings = new ArrayList<>();

      while (rs.next()) {
        connectionStrings.add(rs.getNString("connectionString"));
      }

      return connectionStrings;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Tenant> getTenantConfigs() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT json FROM [dbo].[tenantConfig]");

      ResultSet rs = ps.executeQuery();
      var tenants = new ArrayList<Tenant>();

      while(rs.next()) {
        var json = rs.getString(1);
        var tenant = mapper.readValue(json, Tenant.class);
        tenants.add(tenant);
      }

      return tenants;
    } catch (SQLException | NullPointerException | JsonProcessingException e) {
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
    } catch (SQLException | NullPointerException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MeasureDefinition getMeasureDefinition(String measureId) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT id, bundle, lastUpdated FROM [dbo].[measureDef] WHERE measureId = ?");
      ps.setNString(1, measureId);

      ResultSet rs = ps.executeQuery();

      MeasureDefinition measureDef = null;

      if(rs.next()) {
        measureDef = new MeasureDefinition();
        String id = rs.getObject(1, UUID.class).toString();
        var json = rs.getString(2);
        java.util.Date lastUpdated  = new java.util.Date(rs.getTimestamp(3).getTime());
        measureDef.setId(id);
        measureDef.setBundle(FhirContextProvider.getFhirContext().newJsonParser().parseResource(Bundle.class, json));
        measureDef.setMeasureId(measureId);
        measureDef.setLastUpdated(lastUpdated);

        assert measureDef.getBundle() != null;
        assert measureDef.getMeasureId() != null;
      }

      return measureDef;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public List<MeasureDefinition> getMeasureDefinitions() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT bundle, lastUpdated, measureId FROM [dbo].[measureDef]");

      ResultSet rs = ps.executeQuery();
      var measureDefs = new ArrayList<MeasureDefinition>();

      while(rs.next()) {
        var measureDef = new MeasureDefinition();
        var json = rs.getString(1);
        java.util.Date lastUpdated  = rs.getTimestamp(2);
        var measureId = rs.getString(3);

        measureDef.setBundle(FhirContextProvider.getFhirContext().newJsonParser().parseResource(Bundle.class, json));
        measureDef.setMeasureId(measureId);
        measureDef.setLastUpdated(lastUpdated);
        measureDefs.add(measureDef);
      }

      return measureDefs;
    } catch (SQLException | NullPointerException e) {
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
      cs.setNString("bundle", FhirContextProvider.getFhirContext().newJsonParser().encodeResourceToString(measureDefinition.getBundle()));
      cs.setDateTime("lastUpdated", measureDefinition.getLastUpdated().getTime());

      cs.executeUpdate();

    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
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
        var json = rs.getString(1);
        measurePackage = mapper.readValue(json, MeasurePackage.class);
        measurePackage.setId(packageId);
      }

      assert measurePackage != null;

      return measurePackage;

    } catch (SQLException | NullPointerException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public List<MeasurePackage> getMeasurePackages() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT measures FROM [dbo].[measurePackage]");

      ResultSet rs = ps.executeQuery();
      var packages = new ArrayList<MeasurePackage>();

      while(rs.next()) {
        var json = rs.getString(1);
        var measurePackage = mapper.readValue(json, MeasurePackage.class);
        packages.add(measurePackage);
      }

      return packages;
    } catch (SQLException | NullPointerException | JsonProcessingException e) {
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
    } catch (SQLException | NullPointerException | JsonProcessingException e) {
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

  public void saveMetrics(List<Metrics> metrics){
    metrics.forEach(metric -> {
      try (Connection conn = this.getSQLConnection()) {
        assert conn != null;
        SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveMetrics (?, ?, ?, ?, ?, ?, ?) }");
        cs.setNString("id", metric.getId().toString());
        cs.setNString("tenantId", metric.getTenantId());
        cs.setNString("reportId", metric.getReportId());
        cs.setNString("category", metric.getCategory().toString());
        cs.setNString("taskName", metric.getTaskName().toString());
        cs.setNString("timestamp", metric.getTimestamp().toString());
        cs.setNString("data", mapper.writeValueAsString(metric.getData()));

        cs.executeUpdate();
      } catch(SQLServerException e){
        SQLServerHelper.handleException(e);
      } catch (SQLException | NullPointerException | JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public List<Metrics> getMetrics(Date start, Date end) {
    if(start.compareTo(end) > 0){
      throw new RuntimeException("Start date must be before end date");
    }

    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [dbo].[metrics] WHERE timestamp > ? AND timestamp < ?");
      ps.setNString(1, start.toString());
      ps.setNString(2, end.toString());

      ResultSet rs = ps.executeQuery();
      var metrics = new ArrayList<Metrics>();

      while(rs.next()) {
        Metrics metric = new Metrics();
        metric.setId(rs.getObject(1, UUID.class));
        metric.setTenantId(rs.getString(2));
        metric.setReportId(rs.getString(3));
        metric.setCategory(rs.getString(4));
        metric.setTaskName(rs.getString(5));
        metric.setTimestamp(new Date(rs.getString(6)));
        metric.setData(mapper.readValue(rs.getString(7), MetricData.class));

        metrics.add(metric);
      }

      return metrics;
    } catch (SQLException | NullPointerException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Audit> getAudits() {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [dbo].[audit]");

      ResultSet rs = ps.executeQuery();
      var audits = new ArrayList<Audit>();

      while(rs.next()) {
        var iD = rs.getObject(1, UUID.class);
        var network = rs.getString(2);
        var notes = rs.getString(3);
        var tenantId = rs.getString(4);
        var timeStamp = rs.getDate(5);
        var type = rs.getString(6);
        var userId = rs.getObject(7, UUID.class);

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
      cs.setUUID("id", audit.getId());
      cs.setNString("network", audit.getNetwork());
      cs.setNString("notes", audit.getNotes());
      cs.setNString("tenantId", audit.getTenantId());
      cs.setDateTime("timeStamp", audit.getTimestamp().getTime());
      cs.setNString("type", audit.getType().toString());
      cs.setUUID("userID", audit.getUserId());

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
      cs.setUUID("id", user.getId());
      cs.setNString("email", user.getEmail());
      cs.setNString("name", user.getName());
      cs.setBoolean("enabled", user.getEnabled());
      cs.setNString("passwordHash", user.getPasswordHash());
      cs.setBytes("passwordSalt", user.getPasswordSalt());

      try (ResultSet rs = cs.executeQuery()) {
        if (rs.next()) {
          user.setId(rs.getObject(1, UUID.class));
        }
      }
    } catch (SQLServerException e) {
      SQLServerHelper.handleException(e);
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public User getUser(UUID id) {
    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT * FROM [user] WHERE id = ?");
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

  public List<String> getAllDatabaseNames(String excludeTenantId) {
    List<String> databaseNames = new ArrayList<>();
    databaseNames.add(Helper.getDatabaseName(this.config.getConnectionString()));
    for (String connectionString : this.getTenantConnectionStrings(excludeTenantId)) {
      String tenantDatabaseName = Helper.getDatabaseName(connectionString);
      databaseNames.add(tenantDatabaseName);
    }
    return databaseNames;
  }

  public List<LogMessage> findLogMessages(Date startDate, Date endDate, String[] severities, int page, String content) {
    int countPerPage = 10;

    if (severities != null && severities.length > 0) {
      for (String s : severities) {
        if (Arrays.stream(LogMessage.SEVERITIES).noneMatch(s::equals)) {
          throw new IllegalArgumentException("Invalid severity, must be one of " + String.join(", ", LogMessage.SEVERITIES));
        }
      }
    }

    try (Connection conn = this.getSQLConnection()) {
      assert conn != null;

      String sql = "SELECT * FROM [logging_event] WHERE event_id IS NOT NULL";
      List<Object> params = new ArrayList<>();

      if (startDate != null) {
        sql += " AND timestmp >= ?";
        params.add(BigDecimal.valueOf(startDate.getTime()));
      }

      if (endDate != null) {
        sql += " AND timestmp <= ?";
        params.add(BigDecimal.valueOf(endDate.getTime()));
      }

      if (severities != null && severities.length > 0) {
        sql += " AND level_string IN ('";
        sql += String.join("','", severities);
        sql += "')";
      }

      if (content != null && !content.isEmpty()) {
        sql += " AND formatted_message LIKE ?";
        params.add("%" + content + "%");
      }

      sql += " ORDER BY timestmp DESC";

      // paging
      sql += " OFFSET ? ROWS FETCH NEXT " + countPerPage + " ROWS ONLY";
      params.add((page - 1) * countPerPage);

      PreparedStatement ps = conn.prepareStatement(sql);
      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }

      ResultSet rs = ps.executeQuery();
      List<LogMessage> logMessages = new ArrayList<>();

      while (rs.next()) {
        LogMessage next = LogMessage.create(rs);
        logMessages.add(next);
      }

      return logMessages;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }
}
