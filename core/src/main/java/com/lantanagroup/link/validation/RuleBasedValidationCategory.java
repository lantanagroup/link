package com.lantanagroup.link.validation;

import com.lantanagroup.link.model.ValidationCategory;
import com.lantanagroup.link.model.ValidationCategorySeverities;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RuleBasedValidationCategory extends ValidationCategory {
  private List<ValidationCategoryRuleSet> ruleSets = new ArrayList<>();

  public RuleBasedValidationCategory(String title, ValidationCategorySeverities severity, Boolean acceptable, String guidance) {
    super(title, severity, acceptable, guidance);
  }

  public ValidationCategoryRuleSet addRuleSet() {
    return this.addRuleSet(true);
  }

  public RuleBasedValidationCategory addRuleSet(boolean isAndOperator, List<ValidationCategoryRule> rules) {
    ValidationCategoryRuleSet ruleSet = new ValidationCategoryRuleSet(isAndOperator, rules);
    this.ruleSets.add(ruleSet);
    return this;
  }

  public ValidationCategoryRuleSet addRuleSet(boolean isAndOperator) {
    ValidationCategoryRuleSet ruleSet = new ValidationCategoryRuleSet(isAndOperator);
    this.ruleSets.add(ruleSet);
    return ruleSet;
  }
}
