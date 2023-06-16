package com.lantanagroup.link.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLHelper {
  public static String getNString(ResultSet rs, String label) throws SQLException {
    int index = rs.findColumn(label);
    if (index > 0) {
      return rs.getNString(index);
    }
    return null;
  }

  public static String getString(ResultSet rs, String label) throws SQLException {
    int index = rs.findColumn(label);
    if (index > 0) {
      return rs.getString(index);
    }
    return null;
  }

  public static Boolean getBoolean(ResultSet rs, String label) throws SQLException {
    int index = rs.findColumn(label);
    if (index > 0) {
      return rs.getBoolean(index);
    }
    return null;
  }
}
