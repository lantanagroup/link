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
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.conceptMap;");
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public ConceptMap findById(String id) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM dbo.conceptMap WHERE id = ?;")) {
      statement.setNString(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  private int insert(ConceptMap conceptMap, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.conceptMap " +
            "(id, contexts, conceptMap) " +
            "VALUES " +
            "(?, ?, ?);")) {
      statement.setNString(1, conceptMap.getId());
      statement.setNString(2, serializeList(conceptMap.getContexts()));
      statement.setNString(3, serializeResource(conceptMap.getConceptMap()));
      return statement.executeUpdate();
    }
  }

  private int update(ConceptMap conceptMap, Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("UPDATE dbo.conceptMap " +
            "SET contexts = ?, conceptMap = ? " +
            "WHERE id = ?;")) {
      statement.setNString(1, serializeList(conceptMap.getContexts()));
      statement.setNString(2, serializeResource(conceptMap.getConceptMap()));
      statement.setNString(3, conceptMap.getId());
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
}
