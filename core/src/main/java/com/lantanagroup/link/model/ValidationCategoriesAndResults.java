package com.lantanagroup.link.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.lantanagroup.link.db.model.Report;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Device;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JacksonXmlRootElement(localName = "validation")
public class ValidationCategoriesAndResults {
  @JacksonXmlProperty(localName = "report")
  public Report report;

  public Device device;

  @JacksonXmlProperty(localName = "category")
  @JacksonXmlElementWrapper(localName = "categories")
  public List<ValidationCategoryResponse> categories = new ArrayList<>();

  @JacksonXmlProperty(localName = "result")
  @JacksonXmlElementWrapper(localName = "results")
  public List<ValidationResultResponse> results = new ArrayList<>();

  public ValidationCategoriesAndResults(Report report) {
    this.report = new Report();
    this.report.setId(report.getId());
    this.report.setMeasureIds(report.getMeasureIds());
    this.report.setPeriodStart(report.getPeriodStart());
    this.report.setPeriodEnd(report.getPeriodEnd());
    this.report.setStatus(report.getStatus());
    this.report.setGeneratedTime(report.getGeneratedTime());
    this.report.setSubmittedTime(report.getSubmittedTime());
    //this.device = report.getDeviceInfo();
  }

  public Boolean isPreQualified() {
    return this.categories.stream().noneMatch(c -> !c.getAcceptable() && c.getCount() > 0);
  }
}
