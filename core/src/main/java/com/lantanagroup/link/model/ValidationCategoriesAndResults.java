package com.lantanagroup.link.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JacksonXmlRootElement(localName = "validation")
public class ValidationCategoriesAndResults {
  @JacksonXmlProperty(localName = "category")
  @JacksonXmlElementWrapper(localName = "categories")
  public List<ValidationCategoryResponse> categories = new ArrayList<>();

  @JacksonXmlProperty(localName = "result")
  @JacksonXmlElementWrapper(localName = "results")
  public List<ValidationResultResponse> results = new ArrayList<>();
}
