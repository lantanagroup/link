package com.lantanagroup.link.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.model.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
@Setter
public class ValidationCategorizer {
  private static final Logger logger = LoggerFactory.getLogger(ValidationCategorizer.class.getName());

  private List<RuleBasedValidationCategory> categories = new ArrayList<>();

  public static List<ValidationCategory> loadAndRetrieveCategories() {
    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.loadFromResources();
    return categorizer.categories.stream().map(c -> (ValidationCategory) c).collect(Collectors.toList());
  }

  public RuleBasedValidationCategory addCategory(String title, ValidationCategorySeverities severity, Boolean acceptable, String guidance) {
    RuleBasedValidationCategory category = new RuleBasedValidationCategory(title, severity, acceptable, guidance);
    this.categories.add(category);
    return category;
  }

  public void loadFromResources() {
    try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("validation-categories.json")) {
      String json = Helper.readInputStream(is);
      this.categories = List.of(new ObjectMapper().readValue(json, RuleBasedValidationCategory[].class));
    } catch (IOException e) {
      logger.error("Error loading validation categories from resources", e);
    }
  }

  public boolean isMatch(ValidationCategoryRule rule, Issue issue) {
    Predicate<String> predicate = value -> {
      boolean result = rule.getPattern().matcher(value).find();
      if (rule.isInverse()) {
        result = !result;
      }
      return result;
    };
    switch (rule.getField()) {
      case SEVERITY:
        return predicate.test(issue.getSeverity());
      case CODE:
        return predicate.test(issue.getCode());
      case DETAILS_TEXT:
        return predicate.test(issue.getDetails());
      case EXPRESSION:
        return predicate.test(issue.getExpression());
      default:
        return false;
    }
  }

  public boolean isMatch(ValidationCategoryRuleSet ruleSet, Issue issue) {
    List<Boolean> results = ruleSet.getRules().stream()
            .map(rule -> isMatch(rule, issue))
            .collect(Collectors.toList());
    return ruleSet.isAndOperator() ? !results.contains(false) : results.contains(true);
  }

  public boolean isMatch(RuleBasedValidationCategory category, Issue issue) {
    return category.getRuleSets().stream().allMatch(ruleSet -> isMatch(ruleSet, issue));
  }

  public List<String> categorize(Issue issue) {
    return this.categories.stream()
            .filter(category -> isMatch(category, issue))
            .map(ValidationCategory::getId)
            .collect(Collectors.toList());
  }

  public List<ValidationResultCategory> categorize(List<ValidationResult> results) {
    List<ValidationResultCategory> resultCategories = new ArrayList<>();

    if (results == null) {
      return resultCategories;
    }

    for (ValidationResult result : results) {
      Issue issue = new Issue(result);
      for (String categoryCode : categorize(issue)) {
        ValidationResultCategory category = new ValidationResultCategory();
        category.setValidationResultId(result.getId());
        category.setCategoryCode(categoryCode);
        resultCategories.add(category);
      }
    }

    return resultCategories;
  }

  public static ValidationCategoryResponse buildUncategorizedCategory(int count) {
    ValidationCategoryResponse response = new ValidationCategoryResponse();
    response.setId("uncategorized");
    response.setTitle("Uncategorized");
    response.setSeverity(ValidationCategorySeverities.WARNING);
    response.setAcceptable(false);
    response.setGuidance("These issues need to be categorized.");
    response.setCount(count);
    return response;
  }

  public ValidationCategoriesAndResults getValidationCategoriesAndResults(TenantService tenantService, Report report) {
    ValidationCategoriesAndResults categoriesAndResults = new ValidationCategoriesAndResults(report);
    List<ValidationCategory> categories = ValidationCategorizer.loadAndRetrieveCategories();
    List<ValidationResult> results = tenantService.getValidationResults(report.getId());
    List<ValidationResultCategory> resultCategories = tenantService.findValidationResultCategoriesByReport(report.getId());

    categoriesAndResults.setCategories(categories.stream()
            .map(c -> {
              ValidationCategoryResponse response = new ValidationCategoryResponse(c);
              response.setCount(resultCategories.stream().filter(rc -> rc.getCategoryCode().equals(c.getId())).count());
              return response;
            })
            .filter(c -> c.getCount() > 0)
            .collect(Collectors.toList()));

    if (results.stream().anyMatch(r -> resultCategories.stream().noneMatch(rc -> rc.getValidationResultId().equals(r.getId())))) {
      categoriesAndResults.getCategories().add(buildUncategorizedCategory(tenantService.countUncategorizedValidationResults(report.getId())));
    }

    categoriesAndResults.setResults(results.stream().map(r -> {
      ValidationResultResponse resultResponse = new ValidationResultResponse();
      resultResponse.setId(r.getId());
      resultResponse.setCode(r.getCode());
      resultResponse.setDetails(r.getDetails());
      resultResponse.setSeverity(r.getSeverity());
      resultResponse.setExpression(r.getExpression());
      resultResponse.setPosition(r.getPosition());
      resultResponse.setCategories(resultCategories.stream()
              .filter(rc -> rc.getValidationResultId().equals(r.getId()))
              .map(ValidationResultCategory::getCategoryCode)
              .collect(Collectors.toList()));

      if (resultResponse.getCategories().isEmpty()) {
        resultResponse.getCategories().add("uncategorized");
      }

      return resultResponse;
    }).collect(Collectors.toList()));

    return categoriesAndResults;
  }

  public String getValidationCategoriesAndResultsHtml(TenantService tenantService, Report report) throws IOException {
    ValidationCategoriesAndResults categoriesAndResults = this.getValidationCategoriesAndResults(tenantService, report);

    if (categoriesAndResults.getResults() != null && categoriesAndResults.getResults().isEmpty()) {
      return null;
    }

    ObjectMapper mapper = new ObjectMapper();

    try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("validation-categories.html")) {
      String json = mapper.writeValueAsString(categoriesAndResults);
      String html = Helper.readInputStream(is);
      return html.replace("var report = {};", "var report = " + json + ";");
    }
  }

  @Getter
  public static class Issue {
    private final String severity;
    private final String code;
    private final String details;
    private final String expression;

    public Issue(ValidationResult result) {
      this.severity = result.getSeverity();
      this.code = result.getCode();
      this.details = result.getDetails();
      this.expression = result.getExpression();
    }
  }
}
