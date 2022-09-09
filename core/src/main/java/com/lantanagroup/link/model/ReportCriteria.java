package com.lantanagroup.link.model;

import com.lantanagroup.link.auth.LinkCredentials;
import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

@Getter
@Setter
public class ReportCriteria {

  String reportDefIdentifier;
  String reportDefId;  // TODO: Remove? Currently unused (though that may change as part of LINK-1151)
  String periodStart;
  String periodEnd;
  String measureId;  // TODO: Remove? Currently unused
  HashMap<String, String> additional = new HashMap<>();  // TODO: Remove? Currently unused

  public ReportCriteria(String reportDefIdentifier, String periodStart, String periodEnd) {
    this.setReportDefIdentifier(reportDefIdentifier);
    this.setPeriodStart(periodStart);
    this.setPeriodEnd(periodEnd);
  }
}
