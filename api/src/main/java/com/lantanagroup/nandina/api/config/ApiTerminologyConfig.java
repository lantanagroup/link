package com.lantanagroup.nandina.api.config;

import com.lantanagroup.nandina.config.ITerminologyConfig;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApiTerminologyConfig implements ITerminologyConfig {
  private String covidCodesValueSet;
  private String ventilatorCodesValueSet;
  private String intubationProcedureCodesValueSet;
}
