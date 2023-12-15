package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationCategoryResponse extends ValidationCategory {
  private long count;

  public ValidationCategoryResponse(ValidationCategory category) {
    super(category.getTitle(), category.getSeverity(), category.getAcceptable(), category.getType(), category.getGuidance());
    this.setId(category.getId());
  }
}
