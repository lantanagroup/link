package com.lantanagroup.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.model.ValidationCategory;
import com.lantanagroup.link.model.ValidationCategorySeverities;
import com.lantanagroup.link.model.ValidationCategoryTypes;
import com.lantanagroup.link.validation.RuleBasedValidationCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class ValidationCategorizer {
  private static final Logger logger = LoggerFactory.getLogger(ValidationCategorizer.class.getName());

  private List<ValidationResult> results;

  @Getter
  @Setter
  private List<RuleBasedValidationCategory> categories = new ArrayList<>();

  public ValidationCategorizer(List<ValidationResult> results) {
    this.results = results;
  }

  public static List<ValidationCategory> loadAndRetrieveCategories() {
    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.loadFromResources();
    return categorizer.categories.stream().map(c -> (ValidationCategory) c).collect(Collectors.toList());
  }

  public RuleBasedValidationCategory addCategory(String title, ValidationCategorySeverities severity, Boolean acceptable, ValidationCategoryTypes type, String guidance) {
    RuleBasedValidationCategory category = new RuleBasedValidationCategory(title, severity, acceptable, type, guidance);
    this.categories.add(category);
    return category;
  }

  public void loadFromResources() {
    try {
      String json = Helper.readInputStream(this.getClass().getClassLoader().getResourceAsStream("validation-categories.json"));
      this.categories = List.of(new ObjectMapper().readValue(json, RuleBasedValidationCategory[].class));
    } catch (IOException e) {
      logger.error("Error loading validation categories from resources", e);
    }
  }

  public List<ValidationResultCategory> categorize() {
    List<ValidationResultCategory> resultCategories = new ArrayList<>();

    if (this.results == null) {
      return resultCategories;
    }

    for (RuleBasedValidationCategory category : this.categories) {
      for (ValidationResult result : this.results) {
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
          validationResultCategory.setCategoryCode(category.getTitle().replaceAll("[^a-zA-Z0-9]", "_"));
          resultCategories.add(validationResultCategory);
        }
      }
    }

    return resultCategories;
  }
}
