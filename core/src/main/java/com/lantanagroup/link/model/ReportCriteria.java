package com.lantanagroup.link.model;

import lombok.Getter;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

@Getter
public class ReportCriteria {
  private final String packageId;
  private final SortedSet<String> bundleIds;
  private final String periodStart;
  private final String periodEnd;

  public ReportCriteria(String packageId, Collection<String> bundleIds, String periodStart, String periodEnd) {
    this.packageId = packageId;
    this.bundleIds = new TreeSet<>(bundleIds);
    // TODO: Reformat dates for consistency/compatibility? Or parse into an actual date type?
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }

  public ReportCriteria(Collection<String> bundleIds, String periodStart, String periodEnd) {
    this(null, bundleIds, periodStart, periodEnd);
  }
}
