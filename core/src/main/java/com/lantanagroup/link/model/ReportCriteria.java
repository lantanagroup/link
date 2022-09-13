package com.lantanagroup.link.model;

import lombok.Getter;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

@Getter
public class ReportCriteria {
  private final SortedSet<String> reportDefIdentifiers;
  private final String periodStart;
  private final String periodEnd;

  public ReportCriteria(Collection<String> reportDefIdentifiers, String periodStart, String periodEnd) {
    this.reportDefIdentifiers = new TreeSet<>(reportDefIdentifiers);
    // TODO: Reformat dates for consistency/compatibility? Or parse into an actual date type?
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }
}
