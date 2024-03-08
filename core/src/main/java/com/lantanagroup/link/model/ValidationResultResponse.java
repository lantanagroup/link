package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResultResponse extends ValidationResult {

  @JacksonXmlProperty(localName = "category")
  @JacksonXmlElementWrapper(localName = "categories")
  private List<String> categories;
}
