package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportCriteria;

import java.util.ArrayList;
import java.util.List;

public class IdentifiersHelper {

  public static String getMasterIdentifierValue(ReportCriteria criteria) {
    List components = new ArrayList(criteria.getBundleIds());
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
}
