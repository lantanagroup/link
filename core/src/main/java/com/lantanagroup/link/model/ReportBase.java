package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Device;

import java.util.List;

@Getter
@Setter
public class ReportBase {
  private String id;
  private List<String> measureIds;
  private String periodStart;
  private String periodEnd;
  private Device deviceInfo;
}
