package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatuses;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.UUID;

public class BulkStatusRepository extends BaseRepository<BulkStatus> {
  private final DataSource dataSource;

  public BulkStatusRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected BulkStatus mapOne(ResultSet resultSet) throws SQLException {
    BulkStatus model = new BulkStatus();
    model.setId(resultSet.getObject("id", UUID.class));
    model.setStatusUrl(resultSet.getNString("statusUrl"));
    model.setStatus(resultSet.getNString("status"));
    model.setDate(resultSet.getTimestamp("date"));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<BulkStatus> findAll() {
    String sql = "SELECT * FROM dbo.bulkStatus";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public BulkStatus findById(UUID id) {
    String sql = "SELECT * FROM dbo.bulkStatus WHERE id = ?";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public List<BulkStatus> findPendingWithUrl() {
    String sql = "SELECT * FROM dbo.bulkStatus WHERE statusUrl IS NOT NULL AND status = ?";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, BulkStatuses.PENDING);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  private int insert(BulkStatus bulkStatus, Connection connection) throws SQLException {
    if (bulkStatus.getId() == null) {
      bulkStatus.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.bulkStatus " +
            "(id, statusUrl, status, date) " +
            "VALUES " +
            "(?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(bulkStatus, statement);
      parameters.addId();
      parameters.addStatusUrl();
      parameters.addStatus();
      parameters.addDate();
      return statement.executeUpdate();
    }
  }

  private int update(BulkStatus bulkStatus, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.bulkStatus " +
            "SET statusUrl = ?, status = ?, date = ? " +
            "WHERE id = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(bulkStatus, statement);
      parameters.addStatusUrl();
      parameters.addStatus();
      parameters.addDate();
      parameters.addId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(BulkStatus bulkStatus) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(bulkStatus, connection) == 0) {
          insert(bulkStatus, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private class Parameters {
    private final BulkStatus model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(BulkStatus model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setObject(nextParameterIndex++, model.getId());
    }

    public void addStatusUrl() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getStatusUrl());
    }

    public void addStatus() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getStatus());
    }

    public void addDate() throws SQLException {
      statement.setTimestamp(nextParameterIndex++, new Timestamp(model.getDate().getTime()));
    }
  }
}
