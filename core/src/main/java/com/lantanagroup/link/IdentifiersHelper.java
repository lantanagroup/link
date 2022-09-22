package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportCriteria;

import java.util.LinkedList;
import java.util.List;

public class IdentifiersHelper {

  public static String getMasterIdentifierValue(ReportCriteria criteria) {
    List components = new LinkedList(criteria.getBundleIds());
    components.add(criteria.getPeriodStart());
    components.add(criteria.getPeriodEnd());
    return Helper.getHashedValue(components);
  }

  public static String getMasterMeasureReportId(String masterIdentifierValue, String measureBundleId) {
    return masterIdentifierValue + "-" + measureBundleId.hashCode();
  }

  public static String getIndividualMeasureReportId(String masterMeasureReportId, String patientId) {
    return masterMeasureReportId + "-" + patientId.hashCode();
  }

  public static String getPatientBundleId(String masterIdentifierValue, String patientId) {
    return masterIdentifierValue + "-" + patientId.hashCode();
  }

  public static String getMasterMeasureReportId(String reportId) {
    String masterMeasureReportId = "";

    if (reportId.charAt(0) == '-') {
      masterMeasureReportId = '-' + reportId.substring(1, reportId.substring(1).indexOf("-") + 1);
    } else {
      masterMeasureReportId = reportId.substring(0, reportId.indexOf("-"));
    }
    return masterMeasureReportId;
  }
}
