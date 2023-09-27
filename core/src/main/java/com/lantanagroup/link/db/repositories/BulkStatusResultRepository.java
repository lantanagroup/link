package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.model.BulkResponse;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class BulkStatusResultRepository extends BaseRepository<BulkStatusResult> {
  private final DataSource dataSource;

  public BulkStatusResultRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected BulkStatusResult mapOne(ResultSet resultSet) throws SQLException {
    BulkStatusResult model = new BulkStatusResult();
    model.setId(resultSet.getObject("id", UUID.class));
    model.setStatusId(resultSet.getObject("statusId", UUID.class));
    model.setResult(deserializeObject(BulkResponse.class, resultSet.getNString("result")));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<BulkStatusResult> findAll() {
    String sql = "SELECT * FROM dbo.bulkStatusResult";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  private int insert(BulkStatusResult bulkStatusResult, Connection connection) throws SQLException {
    if (bulkStatusResult.getId() == null) {
      bulkStatusResult.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.bulkStatusResult " +
            "(id, statusId, result) " +
            "VALUES " +
            "(?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(bulkStatusResult, statement);
      parameters.addId();
      parameters.addStatusId();
      parameters.addResult();
      return statement.executeUpdate();
    }
  }

  private int update(BulkStatusResult bulkStatusResult, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.bulkStatusResult " +
            "SET statusId = ?, result = ? " +
            "WHERE id = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(bulkStatusResult, statement);
      parameters.addStatusId();
      parameters.addResult();
      parameters.addId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(BulkStatusResult bulkStatusResult) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(bulkStatusResult, connection) == 0) {
          insert(bulkStatusResult, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private class Parameters {
    private final BulkStatusResult model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(BulkStatusResult model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setObject(nextParameterIndex++, model.getId());
    }

    public void addStatusId() throws SQLException {
      statement.setObject(nextParameterIndex++, model.getStatusId());
    }

    public void addResult() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeObject(model.getResult()));
    }
  }
}
