package com.lantanagroup.link.db;

import org.apache.commons.text.StringEscapeUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SQLUtils {
  public static String format(Statement statement, String... titles) throws SQLException {
    StringBuilder result = new StringBuilder();
    for (int index = 0; ; index++) {
      String title = titles.length > index ? titles[index] : null;
      ResultSet resultSet = statement.getResultSet();
      if (resultSet != null) {
        result.append(format(resultSet, title));
      }
      if (!statement.getMoreResults() && statement.getUpdateCount() == -1) {
        break;
      }
    }
    return result.toString();
  }

  public static String format(ResultSet resultSet, String title) throws SQLException {
    ResultSetMetaData metadata = resultSet.getMetaData();
    StringBuilder result = new StringBuilder();
    if (title != null) {
      result.append(getTag("h2", title));
    }
    result.append("<table><thead>");
    result.append(getHeaderRow(metadata));
    result.append("</thead><tbody>");
    while (resultSet.next()) {
      result.append(getDataRow(resultSet, metadata));
    }
    result.append("</tbody></table>");
    return result.toString();
  }

  private static String getHeaderRow(ResultSetMetaData metadata) throws SQLException {
    return String.format("<tr>%s</tr>", IntStream.rangeClosed(1, metadata.getColumnCount())
            .mapToObj(column -> {
              try {
                return metadata.getColumnName(column);
              } catch (SQLException e) {
                return "Column" + column;
              }
            })
            .map(content -> getTag("th", content))
            .collect(Collectors.joining()));
  }

  private static String getDataRow(ResultSet resultSet, ResultSetMetaData metadata) throws SQLException {
    return String.format("<tr>%s</tr>", IntStream.rangeClosed(1, metadata.getColumnCount())
            .mapToObj(column -> {
              try {
                return resultSet.getObject(column);
              } catch (SQLException e) {
                return null;
              }
            })
            .map(object -> Objects.toString(object, "NULL"))
            .map(content -> getTag("td", content))
            .collect(Collectors.joining()));
  }

  private static String getTag(String tag, String content) {
    return String.format("<%s>%s</%s>", tag, StringEscapeUtils.escapeHtml4(content), tag);
  }
}
