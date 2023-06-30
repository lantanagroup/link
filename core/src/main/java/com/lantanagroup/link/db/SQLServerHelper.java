package com.lantanagroup.link.db;

import com.microsoft.sqlserver.jdbc.SQLServerError;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SQLServerHelper {
  public static void handleException(SQLServerException e) {
    SQLServerError error = e.getSQLServerError();
    if (error != null && error.getErrorNumber() > 50000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    throw new RuntimeException(e);
  }
}
