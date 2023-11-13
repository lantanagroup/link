package com.lantanagroup.link.db;

import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.UUID;

public class SQLCSHelper {
  private final CallableStatement callableStatement;

  public SQLCSHelper(Connection conn, String statement) throws SQLException {
    this.callableStatement = conn.prepareCall(statement);
  }

  public boolean execute() throws SQLException {
    return this.callableStatement.execute();
  }

  public ResultSet executeQuery() throws SQLException {
    return this.callableStatement.executeQuery();
  }

  public int executeUpdate() throws SQLException {
    return this.callableStatement.executeUpdate();
  }

  public void setString(String parameterName, String value) throws SQLException {
    if (StringUtils.isEmpty(value)) {
      this.callableStatement.setNull(parameterName, Types.VARCHAR);
    } else {
      this.callableStatement.setString(parameterName, value);
    }
  }

  public void setDateTime(String parameterName, Long value) throws SQLException {
    if (value == null) {
      this.callableStatement.setNull(parameterName, Types.TIMESTAMP);
    } else {
      this.callableStatement.setTimestamp(parameterName, new java.sql.Timestamp(value));
    }
  }

  public void setNString(String parameterName, String value) throws SQLException {
    if (StringUtils.isEmpty(value)) {
      this.callableStatement.setNull(parameterName, Types.NVARCHAR);
    } else {
      this.callableStatement.setNString(parameterName, value);
    }
  }

  public void setBytes(String parameterName, byte[] value) throws SQLException {
    if (value == null) {
      this.callableStatement.setNull(parameterName, Types.VARBINARY);
    } else {
      this.callableStatement.setBytes(parameterName, value);
    }
  }

  public void setBoolean(String parameterName, Boolean value) throws SQLException {
    if (value == null) {
      this.callableStatement.setNull(parameterName, Types.NVARCHAR);
    } else {
      this.callableStatement.setBoolean(parameterName, value);
    }
  }

  public void setUUID(String parameterName, UUID value) throws SQLException {
    if (value == null) {
      this.callableStatement.setNull(parameterName, Types.CHAR);
    } else {
      this.callableStatement.setObject(parameterName, value);
    }
  }
}
