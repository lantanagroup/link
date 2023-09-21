package com.lantanagroup.link.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.model.BulkResponse;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BulkStatusService {
  private static final Logger logger = LoggerFactory.getLogger(BulkStatusService.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  @Getter
  private Tenant tenantConfig;
  private final DataSource dataSource;

  protected BulkStatusService(Tenant tenantConfig) {
    this.tenantConfig = tenantConfig;

    this.dataSource = DataSourceBuilder.create()
            .type(SQLServerDataSource.class)
            .url(tenantConfig.getConnectionString())
            .build();
  }

  public static BulkStatusService create(SharedService sharedService, String tenantId) {
    var tenantConfig = sharedService.getTenantConfig(tenantId);
    return new BulkStatusService(tenantConfig);
  }

  public static BulkStatusService create(Tenant tenantConfig) {
    return new BulkStatusService(tenantConfig);
  }

  public List<BulkStatus> getBulkStatuses() {
    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT id, tenantId, statusUrl, [status], [date] FROM [dbo].[bulkStatus] WHERE tenantId = ?");
      ps.setNString(1, tenantConfig.getId());

      ResultSet rs = ps.executeQuery();

      var statuses = new ArrayList<BulkStatus>();

      while(rs.next()) {
        var status = new BulkStatus();

        status.setId(rs.getString(1));
        status.setTenantId(rs.getString(2));
        status.setStatusUrl(rs.getString(3));
        status.setStatus(rs.getString(4));
        status.setLastChecked(new java.util.Date(rs.getDate(5).getTime()));

        statuses.add(status);
      }

      assert statuses.size() > 0;

      return statuses;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public BulkStatus getBulkStatusById(String id) {
    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      String query = "SELECT id, tenantId, statusUrl, [status], [date] FROM [dbo].[bulkStatus] WHERE tenantId = ? AND id = ?";
      PreparedStatement ps = conn.prepareStatement(query);
      ps.setNString(1, tenantConfig.getId());
      ps.setNString(2, id);

      ResultSet rs = ps.executeQuery();

      BulkStatus status = null;

      if(rs.next()) {
        status = new BulkStatus();

        status.setId(rs.getString(1));
        status.setTenantId(rs.getString(2));
        status.setStatusUrl(rs.getString(3));
        status.setStatus(rs.getString(4));
        status.setLastChecked(new java.util.Date(rs.getDate(5).getTime()));
      }

      assert status != null;

      return status;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public List<BulkStatusResult> getBulkStatusResults() {
    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT id, statusId, resultJson FROM [dbo].[bulkStatusResult]");

      ResultSet rs = ps.executeQuery();

      var results = new ArrayList<BulkStatusResult>();

      while(rs.next()) {
        var result = new BulkStatusResult();

        result.setId(rs.getString(1));
        result.setStatusId(rs.getString(2));

        var json = rs.getString(3);
        var response = mapper.readValue(json, BulkResponse.class);
        result.setResult(response);

        results.add(result);
      }

      assert results.size() > 0;

      return results;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public BulkStatus saveBulkStatus(BulkStatus bulkStatus) {
    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveBulkStatus (?, ?, ?, ?, ?) }");
      cs.setNString("id", bulkStatus.getId());
      cs.setNString("tenantId", bulkStatus.getTenantId());
      cs.setNString("status", bulkStatus.getStatus());
      cs.setNString("statusUrl", bulkStatus.getStatusUrl());
      cs.setDateTime("date", bulkStatus.getLastChecked().getTime());

      cs.executeUpdate();

      return bulkStatus;

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public List<BulkStatus> getBulkPendingStatusesWithPopulatedUrl() {
    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      PreparedStatement ps = conn.prepareStatement("SELECT id, tenantId, statusUrl, [status], [date] FROM [dbo].[bulkStatus] WHERE tenantId = ? AND [status] = 'Pending' AND statusURL IS NOT NULL");
      ps.setNString(1, tenantConfig.getId());

      ResultSet rs = ps.executeQuery();

      var statuses = new ArrayList<BulkStatus>();

      while(rs.next()) {
        var status = new BulkStatus();

        status.setId(rs.getString(1));
        status.setTenantId(rs.getString(2));
        status.setStatusUrl(rs.getString(3));
        status.setStatus(rs.getString(4));
        status.setLastChecked(new java.util.Date(rs.getDate(5).getTime()));

        statuses.add(status);
      }

      assert statuses.size() > 0;

      return statuses;
    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveBulkStatusResult(BulkStatusResult result) {
    try (Connection conn = this.dataSource.getConnection()) {
      assert conn != null;

      SQLCSHelper cs = new SQLCSHelper(conn, "{ CALL saveBulkStatusResult (?, ?, ?) }");
      cs.setNString("id", result.getId());
      cs.setNString("statusId", result.getId());
      cs.setNString("result", mapper.writeValueAsString(result.getResult()));

      var res = cs.executeUpdate();

    } catch (SQLException | NullPointerException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
