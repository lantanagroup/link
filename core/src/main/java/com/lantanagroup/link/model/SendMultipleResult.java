package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMultipleResult {
  private String reportId;
  private boolean success;
  private String errorMessage;

  public static SendMultipleResult ok(String reportId) {
    SendMultipleResult result = new SendMultipleResult();
    result.reportId = reportId;
    result.success = true;
    return result;
  }

  public static SendMultipleResult fail(String reportId, String errorMessage) {
    SendMultipleResult result = new SendMultipleResult();
    result.reportId = reportId;
    result.success = false;
    result.errorMessage = errorMessage;
    return result;
  }
}
