package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.PatientMeasureReport;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.MeasureReport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PatientMeasureReportRepository extends BaseRepository<PatientMeasureReport> {
  private final DataSource dataSource;

  public PatientMeasureReportRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected PatientMeasureReport mapOne(ResultSet resultSet) throws SQLException {
    PatientMeasureReport model = new PatientMeasureReport();
    model.setId(resultSet.getNString("id"));
    model.setReportId(resultSet.getNString("reportId"));
    model.setMeasureId(resultSet.getNString("measureId"));
    model.setPatientId(resultSet.getNString("patientId"));
    model.setMeasureReport(deserializeResource(MeasureReport.class, resultSet.getNString("measureReport")));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public PatientMeasureReport findById(String id) {
    String sql = "SELECT * FROM dbo.patientMeasureReport WHERE id = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public List<PatientMeasureReport> findByReportId(String reportId) {
    String sql = "SELECT * FROM dbo.patientMeasureReport WHERE reportId = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, reportId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public List<PatientMeasureReport> findByReportIdAndMeasureId(String reportId, String measureId) {
    String sql = "SELECT * FROM dbo.patientMeasureReport WHERE reportId = ? AND measureId = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, reportId);
      statement.setNString(2, measureId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  private int insert(PatientMeasureReport patientMeasureReport, Connection connection) throws SQLException {
    String sql = "INSERT INTO dbo.patientMeasureReport " +
            "(id, reportId, measureId, patientId, measureReport) " +
            "VALUES " +
            "(?, ?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(patientMeasureReport, statement);
      parameters.addId();
      parameters.addReportId();
      parameters.addMeasureId();
      parameters.addPatientId();
      parameters.addMeasureReport();
      return statement.executeUpdate();
    }
  }

  private int update(PatientMeasureReport patientMeasureReport, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.patientMeasureReport " +
            "SET reportId = ?, measureId = ?, patientId = ?, measureReport = ? " +
            "WHERE id = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(patientMeasureReport, statement);
      parameters.addReportId();
      parameters.addMeasureId();
      parameters.addPatientId();
      parameters.addMeasureReport();
      parameters.addId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(PatientMeasureReport patientMeasureReport) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(patientMeasureReport, connection) == 0) {
          insert(patientMeasureReport, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private class Parameters {
    private final PatientMeasureReport model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(PatientMeasureReport model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getId());
    }

    public void addReportId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getReportId());
    }

    public void addMeasureId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getMeasureId());
    }

    public void addPatientId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getPatientId());
    }

    public void addMeasureReport() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeResource(model.getMeasureReport()));
    }
  }
}
