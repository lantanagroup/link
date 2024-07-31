package com.lantanagroup.link.validation;

import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.ValidationCategory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates an HTML pre-qualification report based on validation output (as OperationOutcomes).
 * This is a simplified report in which validation issues are deduplicated.
 */
public class SimplePreQualReport {
  private final String tenantId;
  private final Report report;
  private final ValidationCategorizer categorizer;
  private final Map<String, ValidationCategory> categoriesById;
  private final Map<ValidationCategory, Integer> countsByCategory;
  private final Map<ValidationCategorizer.Issue, Integer> countsByIssue;

  public SimplePreQualReport(String tenantId, Report report) {
    this.tenantId = tenantId;
    this.report = report;
    categorizer = new ValidationCategorizer();
    categorizer.loadFromResources();
    categoriesById = categorizer.getCategories().stream()
            .collect(Collectors.toMap(ValidationCategory::getId, Function.identity()));
    categoriesById.put(null, ValidationCategorizer.buildUncategorizedCategory());
    countsByCategory = new HashMap<>();
    countsByIssue = new HashMap<>();
  }

  /**
   * Categorizes validation issues.
   * Adds categories and (deduplicated) issues to running totals.
   */
  public void add(OperationOutcome operationOutcome) {
    for (OperationOutcome.OperationOutcomeIssueComponent ooIssue : operationOutcome.getIssue()) {
      ValidationCategorizer.Issue issue = new ValidationCategorizer.Issue(ooIssue);
      countsByIssue.merge(issue, 1, Integer::sum);
      List<String> categoryIds = categorizer.categorize(issue);
      List<ValidationCategory> categories;
      if (categoryIds.isEmpty()) {
        categories = List.of(categoriesById.get(null));
      } else {
        categories = categoryIds.stream()
                .map(categoriesById::get)
                .collect(Collectors.toList());
      }
      for (ValidationCategory category : categories) {
        countsByCategory.merge(category, 1, Integer::sum);
      }
    }
  }

  /**
   * Generates the pre-qualification report as an HTML string.
   */
  public String generate() throws IOException {
    Map<String, Object> substitutions = new HashMap<>();
    substitutions.put("tenant", tenantId);
    substitutions.put("report", report.getId());
    substitutions.put("measures", String.join(", ", report.getMeasureIds()));
    substitutions.put("periodStart", report.getPeriodStart());
    substitutions.put("periodEnd", report.getPeriodEnd());
    substitutions.put("preQualified", isPreQualified() ? "Yes" : "No");
    substitutions.put("categoryCount", countsByCategory.size());
    substitutions.put("issueCount", countsByIssue.values().stream().reduce(0, Integer::sum));
    substitutions.put("categories", getCategoryHtml());
    substitutions.put("issues", getIssueHtml());
    StringSubstitutor substitutor = new StringSubstitutor(substitutions, "<!--", "-->");
    String template = IOUtils.resourceToString("/simple-pre-qual.html", StandardCharsets.UTF_8);
    return substitutor.replace(template);
  }

  private boolean isPreQualified() {
    for (Map.Entry<ValidationCategory, Integer> countByCategory : countsByCategory.entrySet()) {
      ValidationCategory category = countByCategory.getKey();
      int count = countByCategory.getValue();
      if (!Boolean.TRUE.equals(category.getAcceptable()) && count > 0) {
        return false;
      }
    }
    return true;
  }

  private String getCategoryHtml() {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<ValidationCategory, Integer> countByCategory : countsByCategory.entrySet()) {
      ValidationCategory category = countByCategory.getKey();
      int count = countByCategory.getValue();
      builder.append("<tr>");
      addCell(builder, category.getTitle());
      addCell(builder, category.getSeverity());
      addCell(builder, category.getAcceptable());
      addCell(builder, category.getGuidance());
      addCell(builder, count);
      builder.append("</tr>");
    }
    return builder.toString();
  }

  private String getIssueHtml() {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<ValidationCategorizer.Issue, Integer> countByIssue : countsByIssue.entrySet()) {
      ValidationCategorizer.Issue issue = countByIssue.getKey();
      int count = countByIssue.getValue();
      builder.append("<tr>");
      addCell(builder, issue.getSeverity());
      addCell(builder, issue.getCode());
      addCell(builder, issue.getDetails());
      addCell(builder, count);
      builder.append("</tr>");
    }
    return builder.toString();
  }

  private void addCell(StringBuilder builder, Object value) {
    builder.append("<td>");
    String string;
    if (value == null) {
      string = "";
    } else if (value instanceof Boolean) {
      string = ((Boolean) value) ? "Yes" : "No";
    } else {
      string = value.toString();
    }
    builder.append(StringEscapeUtils.escapeHtml4(string));
    builder.append("</td>");
  }
}
