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
    String sql = "SELECT * FROM dbo.report;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public Report findById(String id) {
    String sql = "SELECT * FROM dbo.report WHERE id = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public List<Report> findReportsByPatientListId(String patientListId) {
    String sql = "SELECT r.* FROM dbo.reportPatientList rpl" +
            "INNER JOIN dbo.report r ON rpl.reportId = r.id" +
            "WHERE rpl.patientListId = ?";

    try(Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, patientListId);
      try(ResultSet resultSet = statement.executeQuery()){
        return mapAll(resultSet);
      }
    }
  }

  private int insert(Report report, Connection connection) throws SQLException {
    String sql = "INSERT INTO dbo.report " +
            "(id, measureIds, periodStart, periodEnd, status, version, generatedTime, submittedTime) " +
            "VALUES " +
            "(?, ?, ?, ?, ?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(report, statement);
      parameters.addId();
      parameters.addMeasureIds();
      parameters.addPeriodStart();
      parameters.addPeriodEnd();
      parameters.addStatus();
      parameters.addVersion();
      parameters.addGeneratedTime();
      parameters.addSubmittedTime();
      return statement.executeUpdate();
    }
  }

  private int update(Report report, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.report " +
            "SET measureIds = ?, periodStart = ?, periodEnd = ?, status = ?, version = ?, generatedTime = ?, submittedTime = ? " +
            "WHERE id = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(report, statement);
      parameters.addMeasureIds();
      parameters.addPeriodStart();
      parameters.addPeriodEnd();
      parameters.addStatus();
      parameters.addVersion();
      parameters.addGeneratedTime();
      parameters.addSubmittedTime();
      parameters.addId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void deleteReport(Report report) {
    String sql = "DELETE FROM dbo.report WHERE id = ?;";
    try (PreparedStatement statement = dataSource.getConnection().prepareStatement(sql)) {
      statement.setNString(1, report.getId());
      statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void deletePatientLists(Report report) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        deletePatientLists(report, connection);
      } catch (SQLException e) {
        throw e;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public void deleteAllReports(){
    deleteAllPatientLists();
    deleteAllReportsFromReportTable();
  }

  private void deleteAllPatientLists() throws SQLException {
    String sql = "DELETE FROM dbo.reportPatientList";
    try (PreparedStatement statement = dataSource.getConnection().prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private void deleteAllReportsFromReportTable() throws SQLException {
    String sql = "DELETE FROM dbo.report";
    try (PreparedStatement statement = dataSource.getConnection().prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private void deletePatientLists(Report report, Connection connection) throws SQLException {
    String sql = "DELETE FROM dbo.reportPatientList WHERE reportId = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, report.getId());
      statement.executeUpdate();
    }
  }



  private void insertPatientLists(Report report, List<PatientList> patientLists, Connection connection) throws SQLException {
    String sql = "INSERT INTO dbo.reportPatientList (reportId, patientListId) VALUES (?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
          deletePatientLists(report, connection);
          insertPatientLists(report, patientLists, connection);
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

  private class Parameters {
    private final Report model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(Report model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getId());
    }

    public void addMeasureIds() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeList(model.getMeasureIds()));
    }

    public void addPeriodStart() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getPeriodStart());
    }

    public void addPeriodEnd() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getPeriodEnd());
    }

    public void addStatus() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getStatus().toString());
    }

    public void addVersion() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getVersion());
    }

    public void addGeneratedTime() throws SQLException {
      if (model.getGeneratedTime() == null) {
        statement.setNull(nextParameterIndex++, Types.TIMESTAMP);
      } else {
        statement.setTimestamp(nextParameterIndex++, new Timestamp(model.getGeneratedTime().getTime()));
      }
    }

    public void addSubmittedTime() throws SQLException {
      if (model.getSubmittedTime() == null) {
        statement.setNull(nextParameterIndex++, Types.TIMESTAMP);
      } else {
        statement.setTimestamp(nextParameterIndex++, new Timestamp(model.getSubmittedTime().getTime()));
      }
    }
  }
}
