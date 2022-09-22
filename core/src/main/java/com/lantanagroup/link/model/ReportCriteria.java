package com.lantanagroup.link.model;

import lombok.Getter;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

@Getter
public class ReportCriteria {
  private final SortedSet<String> bundleIds;
  private final String periodStart;
  private final String periodEnd;

  public ReportCriteria(Collection<String> bundleIds, String periodStart, String periodEnd) {
    this.bundleIds = new TreeSet<>(bundleIds);
    // TODO: Reformat dates for consistency/compatibility? Or parse into an actual date type?
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }
}
