package com.lantanagroup.link.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class ValidationCategoryRule {
  private Field field;
  private String regex;
  private boolean isInverse = false;

  @JsonIgnore
  private Pattern pattern;

  public ValidationCategoryRule(Field field, String regex) {
    this.field = field;
    this.regex = regex;
  }

  public ValidationCategoryRule(Field field, String regex, boolean isInverse) {
    this.field = field;
    this.regex = regex;
    this.isInverse = isInverse;
  }

  public Pattern getPattern() {
    if (this.pattern == null) {
      this.pattern = Pattern.compile(this.regex, Pattern.MULTILINE);
    }
    return this.pattern;
  }

  public enum Field {
    DETAILS_TEXT,
    EXPRESSION,
    SEVERITY,
    CODE
  }
}
