package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.ITerminologyConfig;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApiTerminologyConfig implements ITerminologyConfig {
  private String covidCodesValueSet;
  private String ventilatorCodesValueSet;
  private String intubationProcedureCodesValueSet;
}
