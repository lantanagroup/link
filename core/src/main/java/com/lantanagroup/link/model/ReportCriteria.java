package com.lantanagroup.link.model;

import com.lantanagroup.link.StreamUtils;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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

  public ReportCriteria(String bundleId, String periodStart, String periodEnd) {
    this(null, List.of(bundleId), periodStart, periodEnd);
  }

  public String getQueryPlanId() {
    if (packageId != null) {
      return packageId;
    }
    return bundleIds.stream()
            .reduce(StreamUtils::toOnlyElement)
            .orElseThrow();
  }

  public LocalDate getPeriodStartDate() {
    return getDate(periodStart);
  }

  public LocalDate getPeriodEndDate() {
    return getDate(periodEnd);
  }

  private LocalDate getDate(String text) {
    return LocalDate.parse(text.substring(0, 10));
  }
}
