package com.lantanagroup.link.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValidationCategoryRuleSet {
  private boolean isAndOperator = true;
  private List<ValidationCategoryRule> rules = new ArrayList<>();

  public ValidationCategoryRuleSet(boolean isAndOperator) {
    this.isAndOperator = isAndOperator;
  }

  public ValidationCategoryRuleSet addRule(ValidationCategoryRule.Field field, String regex) {
    ValidationCategoryRule rule = new ValidationCategoryRule(field, regex);
    this.rules.add(rule);
    return this;
  }

  public ValidationCategoryRuleSet addRule(ValidationCategoryRule.Field field, String regex, boolean isInverse) {
    ValidationCategoryRule rule = new ValidationCategoryRule(field, regex, isInverse);
    this.rules.add(rule);
    return this;
  }
}
