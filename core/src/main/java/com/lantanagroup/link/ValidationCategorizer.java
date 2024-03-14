package com.lantanagroup.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.model.ValidationCategory;
import com.lantanagroup.link.model.ValidationCategorySeverities;
import com.lantanagroup.link.validation.RuleBasedValidationCategory;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationCategorizer {
  private static final Logger logger = LoggerFactory.getLogger(ValidationCategorizer.class.getName());

  @Getter
  @Setter
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

  public List<ValidationResultCategory> categorize(List<ValidationResult> results) {
    List<ValidationResultCategory> resultCategories = new ArrayList<>();

    if (results == null) {
      return resultCategories;
    }

    for (RuleBasedValidationCategory category : this.categories) {
      for (ValidationResult result : results) {
        boolean allTrueInCategory = category.getRuleSets().stream().allMatch(ruleSet -> {
          List<Boolean> ruleSetResults = ruleSet.getRules().stream().map(rule -> {
            boolean isMatch = false;
            switch (rule.getField()) {
              case SEVERITY:
                isMatch = rule.getPattern().matcher(result.getSeverity()).find();
                break;
              case CODE:
                isMatch = rule.getPattern().matcher(result.getCode()).find();
                break;
              case DETAILS_TEXT:
                isMatch = rule.getPattern().matcher(result.getDetails()).find();
                break;
              case EXPRESSION:
                isMatch = rule.getPattern().matcher(result.getExpression()).find();
                break;
              default:
                return false;
            }

            return rule.isInverse() != isMatch;
          }).collect(Collectors.toList());

          if (ruleSet.isAndOperator()) {
            return ruleSetResults.stream().allMatch(r -> r);
          }

          return ruleSetResults.stream().anyMatch(r -> r);
        });

        if (allTrueInCategory) {
          ValidationResultCategory validationResultCategory = new ValidationResultCategory();
          validationResultCategory.setValidationResultId(result.getId());

          // Because the categories are defined in the shared DB and the validation results are stored in the tenant DB,
          // using a simplified version of the title of the category to associate categorized results to categories. This
          // way, if the shared db's categories changes, it won't inadvertently change the category of previously categorized
          // results. This is a bit of a hack, but it works.
          validationResultCategory.setCategoryCode(category.getId());
          resultCategories.add(validationResultCategory);
        }
      }
    }

    return resultCategories;
  }
}
