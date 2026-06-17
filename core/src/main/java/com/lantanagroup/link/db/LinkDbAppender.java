package com.lantanagroup.link.db;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class LinkDbAppender extends AppenderBase<ILoggingEvent> {

  private static final String INSERT_SQL =
      "INSERT INTO [logging_event] " +
      "(timestmp, formatted_message, logger_name, level_string, thread_name, " +
      " reference_flag, arg0, arg1, arg2, arg3, " +
      " caller_filename, caller_class, caller_method, caller_line) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final DataSource dataSource;

  public LinkDbAppender(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected void append(ILoggingEvent event) {
    StackTraceElement caller = null;
    StackTraceElement[] callerData = event.getCallerData();
    if (callerData != null && callerData.length > 0) {
      caller = callerData[0];
    }

    String callerFilename = caller != null && caller.getFileName() != null ? caller.getFileName() : "";
    String callerClass    = caller != null && caller.getClassName()  != null ? caller.getClassName()  : "";
    String callerMethod   = caller != null && caller.getMethodName() != null ? caller.getMethodName() : "";
    String callerLine     = caller != null ? String.valueOf(caller.getLineNumber()) : "0";
    if (callerLine.length() > 4) callerLine = callerLine.substring(0, 4);

    Object[] args = event.getArgumentArray();

    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

      ps.setBigDecimal(1, BigDecimal.valueOf(event.getTimeStamp()));
      ps.setString(2,  cap(event.getFormattedMessage(), 4000));
      ps.setString(3,  cap(event.getLoggerName(), 254));
      ps.setString(4,  cap(event.getLevel().toString(), 254));
      ps.setString(5,  cap(event.getThreadName(), 254));
      ps.setNull(6, Types.SMALLINT);
      ps.setString(7,  argAt(args, 0));
      ps.setString(8,  argAt(args, 1));
      ps.setString(9,  argAt(args, 2));
      ps.setString(10, argAt(args, 3));
      ps.setString(11, cap(callerFilename, 254));
      ps.setString(12, cap(callerClass, 254));
      ps.setString(13, cap(callerMethod, 254));
      ps.setString(14, callerLine);
      ps.executeUpdate();
    } catch (SQLException e) {
      addError("Failed to write log event to database", e);
    }
  }

  private static String cap(String value, int max) {
    if (value == null) return "";
    return value.length() > max ? value.substring(0, max) : value;
  }

  private static String argAt(Object[] args, int index) {
    if (args == null || index >= args.length) return null;
    Object arg = args[index];
    return arg == null ? null : cap(arg.toString(), 254);
  }
}
