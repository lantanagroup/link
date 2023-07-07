package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.PatientId;
import com.lantanagroup.link.db.model.PatientList;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.UUID;

public class PatientListRepository extends BaseRepository<PatientList> {
  private final DataSource dataSource;

  public PatientListRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  @SneakyThrows(SQLException.class)
  protected PatientList mapOne(ResultSet resultSet) {
    PatientList model = new PatientList();
    model.setId(resultSet.getObject("id", UUID.class));
    model.setMeasureId(resultSet.getNString("measureId"));
    model.setPeriodStart(resultSet.getNString("periodStart"));
    model.setPeriodEnd(resultSet.getNString("periodEnd"));
    model.setPatients(deserializeList(PatientId.class, resultSet.getNString("patients")));
    model.setLastUpdated(resultSet.getTimestamp("lastUpdated"));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<PatientList> findAll() {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.patientList;");
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public PatientList findById(UUID id) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.patientList WHERE id = ?;")) {
      statement.setObject(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public List<PatientList> findByReportId(String reportId) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT pl.* " +
                 "FROM dbo.patientList AS pl " +
                 "INNER JOIN dbo.reportPatientList AS rpl ON pl.id = rpl.patientListId " +
                 "WHERE rpl.reportId = ?;")) {
      statement.setNString(1, reportId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public PatientList findByMeasureIdAndReportingPeriod(String measureId, String periodStart, String periodEnd) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.patientList " +
                 "WHERE measureId = ? AND periodStart = ? AND periodEnd = ?;")) {
      statement.setNString(1, measureId);
      statement.setNString(2, periodStart);
      statement.setNString(3, periodEnd);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  private int insert(PatientList patientList, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.patientList " +
            "(id, measureId, periodStart, periodEnd, patients, lastUpdated) " +
            "VALUES " +
            "(?, ?, ?, ?, ?, ?);")) {
      statement.setObject(1, patientList.getId());
      statement.setNString(2, patientList.getMeasureId());
      statement.setNString(3, patientList.getPeriodStart());
      statement.setNString(4, patientList.getPeriodEnd());
      statement.setNString(5, serializeList(patientList.getPatients()));
      statement.setTimestamp(6, new Timestamp(patientList.getLastUpdated().getTime()));
      return statement.executeUpdate();
    }
  }

  private int update(PatientList patientList, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("UPDATE dbo.patientList " +
            "SET patients = ?, lastUpdated = ? " +
            "WHERE measureId = ? AND periodStart = ? AND periodEnd = ?;")) {
      statement.setNString(1, serializeList(patientList.getPatients()));
      statement.setTimestamp(2, new Timestamp(patientList.getLastUpdated().getTime()));
      statement.setNString(3, patientList.getMeasureId());
      statement.setNString(4, patientList.getPeriodStart());
      statement.setNString(5, patientList.getPeriodEnd());
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(PatientList patientList) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(patientList, connection) == 0) {
          insert(patientList, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public void deleteByIds(List<UUID> ids) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement statement = connection.prepareStatement("DELETE FROM dbo.patientList WHERE id = ?;")) {
        for (UUID id : ids) {
          statement.setObject(1, id);
          statement.executeUpdate();
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }
}
