package com.lantanagroup.link.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogMessage {
  private Date timestamp;
  private String severity;
  private String message;
  private String caller;

  public static final String[] SEVERITIES = new String[]{"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};

  public static LogMessage create(ResultSet rs) throws SQLException {
    LogMessage logMessage = new LogMessage();
    BigDecimal timestamp = rs.getBigDecimal("timestmp");
    logMessage.setTimestamp(new Date(timestamp.longValue()));
    logMessage.setMessage(rs.getString("formatted_message"));
    logMessage.setCaller(rs.getString("caller_class"));
    logMessage.setSeverity(rs.getString("level_string"));
    return logMessage;
  }
}
