package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.PatientData;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PatientDataRepository extends BaseRepository<PatientData> {
  private final DataSource dataSource;

  public PatientDataRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  @SneakyThrows(SQLException.class)
  protected PatientData mapOne(ResultSet resultSet) throws SQLException {
    PatientData model = new PatientData();
    model.setId(resultSet.getObject("id", UUID.class));
    model.setPatientId(resultSet.getNString("patientId"));
    model.setResourceType(resultSet.getNString("resourceType"));
    model.setResourceId(resultSet.getNString("resourceId"));
    model.setResource(deserializeResource(resultSet.getNString("resource")));
    model.setRetrieved(resultSet.getTimestamp("retrieved"));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<PatientData> findByPatientId(String patientId) {
    String sql = "SELECT * FROM dbo.patientData WHERE patientId = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, patientId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  private int insert(PatientData patientData, Connection connection) throws SQLException {
    if (patientData.getId() == null) {
      patientData.setId(UUID.randomUUID());
    }
    String sql = "INSERT INTO dbo.patientData " +
            "(id, patientId, resourceType, resourceId, resource, retrieved) " +
            "VALUES " +
            "(?, ?, ?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(patientData, statement);
      parameters.addId();
      parameters.addPatientId();
      parameters.addResourceType();
      parameters.addResourceId();
      parameters.addResource();
      parameters.addRetrieved();
      return statement.executeUpdate();
    }
  }

  private int update(PatientData patientData, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.patientData " +
            "SET resource = ?, retrieved = ? " +
            "WHERE patientId = ? AND resourceType = ? AND resourceId = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(patientData, statement);
      parameters.addResource();
      parameters.addRetrieved();
      parameters.addPatientId();
      parameters.addResourceType();
      parameters.addResourceId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void saveAll(List<PatientData> patientDatas) {
    try (Connection connection = dataSource.getConnection()) {
      for (PatientData patientData : patientDatas) {
        if (update(patientData, connection) == 0) {
          insert(patientData, connection);
        }
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public int deleteByRetrievedBefore(Date date) {
    String sql = "DELETE FROM dbo.patientData WHERE retrieved < ?";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setTimestamp(1, new Timestamp(date.getTime()));
      return statement.executeUpdate();
    }
  }

  private class Parameters {
    private final PatientData model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(PatientData model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setObject(nextParameterIndex++, model.getId());
    }

    public void addPatientId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getPatientId());
    }

    public void addResourceType() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getResourceType());
    }

    public void addResourceId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getResourceId());
    }

    public void addResource() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeResource(model.getResource()));
    }

    public void addRetrieved() throws SQLException {
      statement.setTimestamp(nextParameterIndex++, new Timestamp(model.getRetrieved().getTime()));
    }
  }
}
