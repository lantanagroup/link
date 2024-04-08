package com.lantanagroup.link.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Device;

import java.util.List;

@Getter
@Setter
public class ReportBase {
  private String id;
  @JacksonXmlProperty(localName = "measureId")
  @JacksonXmlElementWrapper(localName = "measureIds")
  private List<String> measureIds;
  private String periodStart;
  private String periodEnd;
  private Device deviceInfo;
  private String queryPlan;
}
