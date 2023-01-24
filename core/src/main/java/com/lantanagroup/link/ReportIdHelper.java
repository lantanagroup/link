package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ReportIdHelper {
  private static String combine(Iterable<String> components) {
    return String.join("-", components);
  }

  private static String combine(String... components) {
    return combine(List.of(components));
  }

  private static String hash(Iterable<String> components) {
    return Integer.toHexString(combine(components).hashCode());
  }

  public static String hash(String... components) {
    return hash(List.of(components));
  }

  public static String getMasterIdentifierValue(ReportCriteria reportCriteria) {
    Collection<String> components = new LinkedList<>(reportCriteria.getBundleIds());
    components.add(reportCriteria.getPeriodStart());
    components.add(reportCriteria.getPeriodEnd());
    return hash(components);
  }

  public static String getMasterIdentifierValue(String reportId) {
    return reportId.split("-", 2)[0];
  }

  public static String getMasterMeasureReportId(String masterIdentifierValue, String reportBundleId) {
    return combine(masterIdentifierValue, hash(reportBundleId));
  }

  public static String getPatientMeasureReportId(String masterMeasureReportId, String patientId) {
    return combine(masterMeasureReportId, hash(patientId));
  }

  public static String getPatientDataBundleId(String masterIdentifierValue, String patientId) {
    return combine(masterIdentifierValue, hash(patientId));
  }

  public static String getPatientDataBundleId(String patientReportId) {
    String[] ids = patientReportId.split("-");
    return ids.length == 3 ? combine(ids[0], ids[2]) : "";
  }

}
