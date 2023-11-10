package com.lantanagroup.link.db;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.regex.Pattern;

public class SQLScriptExecutor {
  private static final Pattern SEPARATOR = Pattern.compile("(?i)(?:^|\\R)\\s*GO\\s*(?:\\R|$)");
  private static final Logger logger = LoggerFactory.getLogger(SQLScriptExecutor.class);

  public static void execute(Connection connection, Resource script) throws IOException, SQLException {
    logger.debug("Executing script: {}", script);
    String scriptSql;
    try (Reader reader = new EncodedResource(script).getReader()) {
      scriptSql = FileCopyUtils.copyToString(reader);
    }
    for (String statementSql : SEPARATOR.split(scriptSql)) {
      if (StringUtils.isBlank(statementSql)) {
        continue;
      }
      execute(connection, statementSql);
    }
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    logger.trace("Executing SQL: {}", sql);
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
      for (SQLWarning warning = statement.getWarnings(); warning != null; warning = warning.getNextWarning()) {
        logger.warn(warning.getMessage());
      }
    }
  }
}
