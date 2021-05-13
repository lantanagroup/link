package com.lantanagroup.link.query;

import com.lantanagroup.link.config.query.QueryConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.context.ApplicationContext;

public interface IQuery {
  Bundle execute(String[] patientIdentifiers);
  void setApplicationContext(ApplicationContext context);
  void setConfig(QueryConfig config);
}
