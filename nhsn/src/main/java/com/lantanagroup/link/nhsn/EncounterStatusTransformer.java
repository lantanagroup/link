package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;



public class EncounterStatusTransformer implements IReportGenerationEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

  @Autowired
  @Setter
  private ApiConfigEvents apiConfigEvents;

  @Override
  public void execute(ReportCriteria reportCriteria, ReportContext context, ApiConfig config, FhirDataProvider fhirDataProvider) {

    List<String> encounters = null;
    try {
      Method eventMethodInvoked = ApiConfigEvents.class.getMethod("getAfterPatientDataQuery");
      encounters = (List<String>) eventMethodInvoked.invoke(apiConfigEvents);
    } catch (Exception ex) {
      logger.error(String.format("Error in triggerEvent for event AfterPatientDataQuery: ") + ex.getMessage());
    }
    if(encounters != null) {
      for (String encounter : encounters) {

      }
    }
  }
}