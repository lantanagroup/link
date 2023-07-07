package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.ReportStatuses;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

public class ReportRepository extends BaseRepository<Report> {
  private final DataSource dataSource;

  public ReportRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  @SneakyThrows(SQLException.class)
  protected Report mapOne(ResultSet resultSet) throws SQLException {
    Report model = new Report();
    model.setId(resultSet.getNString("id"));
    model.setMeasureIds(deserializeList(String.class, resultSet.getNString("measureIds")));
    model.setPeriodStart(resultSet.getNString("periodStart"));
    model.setPeriodEnd(resultSet.getNString("periodEnd"));
    model.setStatus(ReportStatuses.valueOf(resultSet.getNString("status")));
    model.setVersion(resultSet.getNString("version"));
    model.setGeneratedTime(resultSet.getTimestamp("generatedTime"));
    model.setSubmittedTime(resultSet.getTimestamp("submittedTime"));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<Report> findAll() {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.report;");
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public Report findById(String id) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.report WHERE id = ?;")) {
      statement.setNString(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  private int insert(Report report, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.report " +
            "(id, measureIds, periodStart, periodEnd, status, version, generatedTime, submittedTime) " +
            "VALUES " +
            "(?, ?, ?, ?, ?, ?, ?, ?);")) {
      statement.setNString(1, report.getId());
      statement.setNString(2, serializeList(report.getMeasureIds()));
      statement.setNString(3, report.getPeriodStart());
      statement.setNString(4, report.getPeriodEnd());
      statement.setNString(5, report.getStatus().toString());
      statement.setNString(6, report.getVersion());
      statement.setObject(7, report.getGeneratedTime() == null ? null : new Timestamp(report.getGeneratedTime().getTime()), Types.TIMESTAMP);
      statement.setObject(8, report.getSubmittedTime() == null ? null : new Timestamp(report.getSubmittedTime().getTime()), Types.TIMESTAMP);
      return statement.executeUpdate();
    }
  }

  private int update(Report report, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("UPDATE dbo.report " +
            "SET measureIds = ?, periodStart = ?, periodEnd = ?, status = ?, version = ?, generatedTime = ?, submittedTime = ? " +
            "WHERE id = ?;")) {
      statement.setNString(1, serializeList(report.getMeasureIds()));
      statement.setNString(2, report.getPeriodStart());
      statement.setNString(3, report.getPeriodEnd());
      statement.setNString(4, report.getStatus().toString());
      statement.setNString(5, report.getVersion());
      statement.setObject(6, report.getGeneratedTime() == null ? null : new Timestamp(report.getGeneratedTime().getTime()), Types.TIMESTAMP);
      statement.setObject(7, report.getSubmittedTime() == null ? null : new Timestamp(report.getSubmittedTime().getTime()), Types.TIMESTAMP);
      return statement.executeUpdate();
    }
  }

  private void updatePatientLists(Report report, List<PatientList> patientLists, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM dbo.reportPatientList WHERE reportId = ?;")) {
      statement.setNString(1, report.getId());
      statement.executeUpdate();
    }
    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.reportPatientList " +
            "(reportId, patientListId) " +
            "VALUES " +
            "(?, ?);")) {
      statement.setNString(1, report.getId());
      for (PatientList patientList : patientLists) {
        statement.setObject(2, patientList.getId());
        statement.executeUpdate();
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(Report report, List<PatientList> patientLists) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(report, connection) == 0) {
          insert(report, connection);
        }
        if (patientLists != null) {
          updatePatientLists(report, patientLists, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  public void save(Report report) {
    save(report, null);
  }
}
