package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.ConceptMap;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ConceptMapRepository extends BaseRepository<ConceptMap> {
  private final DataSource dataSource;

  public ConceptMapRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  @SneakyThrows(SQLException.class)
  protected ConceptMap mapOne(ResultSet resultSet) {
    ConceptMap model = new ConceptMap();
    model.setId(resultSet.getString("id"));
    model.setContexts(deserializeList(String.class, resultSet.getNString("contexts")));
    model.setConceptMap(deserializeResource(org.hl7.fhir.r4.model.ConceptMap.class, resultSet.getNString("conceptMap")));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<ConceptMap> findAll() {
    String sql = "SELECT * FROM dbo.conceptMap;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public ConceptMap findById(String id) {
    String sql = "SELECT * FROM dbo.conceptMap WHERE id = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  private int insert(ConceptMap conceptMap, Connection connection) throws SQLException {
    String sql = "INSERT INTO dbo.conceptMap (id, contexts, conceptMap) VALUES (?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(conceptMap, statement);
      parameters.addId();
      parameters.addContexts();
      parameters.addConceptMap();
      return statement.executeUpdate();
    }
  }

  private int update(ConceptMap conceptMap, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.conceptMap SET contexts = ?, conceptMap = ? WHERE id = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(conceptMap, statement);
      parameters.addContexts();
      parameters.addConceptMap();
      parameters.addId();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(ConceptMap conceptMap) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(conceptMap, connection) == 0) {
          insert(conceptMap, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private class Parameters {
    private final ConceptMap model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(ConceptMap model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getId());
    }

    public void addContexts() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeList(model.getContexts()));
    }

    public void addConceptMap() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeResource(model.getConceptMap()));
    }
  }
}
