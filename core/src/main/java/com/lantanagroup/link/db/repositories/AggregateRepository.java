package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.Aggregate;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.MeasureReport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AggregateRepository extends BaseRepository<Aggregate> {
  private final DataSource dataSource;

  public AggregateRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected Aggregate mapOne(ResultSet resultSet) throws SQLException {
    Aggregate model = new Aggregate();
    model.setId(resultSet.getNString("id"));
    model.setReportId(resultSet.getNString("reportId"));
    model.setMeasureId(resultSet.getNString("measureId"));
    model.setReport(deserializeResource(MeasureReport.class, resultSet.getNString("report")));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<Aggregate> findByReportId(String reportId) {
    String sql = "SELECT * FROM dbo.[aggregate] WHERE reportId = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, reportId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  private int insert(Aggregate aggregate, Connection connection) throws SQLException {
    String sql = "INSERT INTO dbo.[aggregate] " +
            "(id, reportId, measureId, report) " +
            "VALUES " +
            "(?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(aggregate, statement);
      parameters.addId();
      parameters.addReportId();
      parameters.addMeasureId();
      parameters.addReport();
      return statement.executeUpdate();
    }
  }

  private int update(Aggregate aggregate, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.[aggregate] " +
            "SET reportId = ?, measureId = ?, report = ? " +
            "WHERE id = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(aggregate, statement);
      parameters.addReportId();
      parameters.addMeasureId();
      parameters.addReport();
      parameters.addId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(Aggregate aggregate) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(aggregate, connection) == 0) {
          insert(aggregate, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private class Parameters {
    private final Aggregate model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(Aggregate model, PreparedStatement statement) {
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

    public void addReport() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeResource(model.getReport()));
    }
  }
}
