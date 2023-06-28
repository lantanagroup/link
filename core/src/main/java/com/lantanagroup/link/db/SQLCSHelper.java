package com.lantanagroup.link.db;

import org.apache.commons.lang3.StringUtils;

import java.sql.*;

public class SQLCSHelper {
  private final CallableStatement callableStatement;

  public SQLCSHelper(Connection conn, String statement) throws SQLException {
    this.callableStatement = conn.prepareCall(statement);
  }

  public ResultSet executeQuery() throws SQLException {
    return this.callableStatement.executeQuery();
  }

  public void setString(String parameterName, String value) throws SQLException {
    if (StringUtils.isEmpty(value)) {
      this.callableStatement.setNull(parameterName, Types.VARCHAR);
    } else {
      this.callableStatement.setString(parameterName, value);
    }
  }

  public void setDateTime(String parameterName, String value) throws SQLException {
    if (StringUtils.isEmpty(value)) {
      this.callableStatement.setNull(parameterName, Types.VARCHAR);
    } else {
      var parsed = java.sql.Date.valueOf(value);
      this.callableStatement.setDate(parameterName, parsed);
    }
  }

  public void setNString(String parameterName, String value) throws SQLException {
    if (StringUtils.isEmpty(value)) {
      this.callableStatement.setNull(parameterName, Types.NVARCHAR);
    } else {
      this.callableStatement.setNString(parameterName, value);
    }
  }

  public void setBytes(String parameterName, String value) throws SQLException {
    if (StringUtils.isEmpty(value)) {
      this.callableStatement.setNull(parameterName, Types.VARBINARY);
    } else {
      this.callableStatement.setBytes(parameterName, value.getBytes());
    }
  }

  public void setBoolean(String parameterName, Boolean value) throws SQLException {
    if (value == null) {
      this.callableStatement.setNull(parameterName, Types.NVARCHAR);
    } else {
      this.callableStatement.setBoolean(parameterName, value);
    }
  }
}
