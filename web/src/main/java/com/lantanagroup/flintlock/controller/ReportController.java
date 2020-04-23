package com.lantanagroup.flintlock.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.flintlock.Config;
import com.lantanagroup.flintlock.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.flintlock.model.QuestionnaireResponseSimple;
import com.lantanagroup.flintlock.query.IQueryCountExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;

@RestController
public class ReportController {

  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  FhirContext ctx = FhirContext.forR4();
  IGenericClient fhirClient;

  public ReportController() {
    this.fhirClient = this.ctx.newRestfulGenericClient(Config.getInstance().getFhirServerBase());
    this.fhirClient.registerInterceptor(new HapiFhirAuthenticationInterceptor());
  }

  /**
   * Uses reflection to determine what class should be used to execute the requested query/className, and
   * executes the specified query, returning the result.
   * @param className
   * @param reportDate
   * @param overflowLocations
   * @return
   */
  private Integer executeQueryCount(String className, String reportDate, String overflowLocations) {
    try {
      Class queryClass = Class.forName(className);
      Constructor queryConstructor = queryClass.getConstructor();
      IQueryCountExecutor executor = (IQueryCountExecutor) queryConstructor.newInstance();
      return executor.execute(Config.getInstance(), this.fhirClient, reportDate, overflowLocations);
    } catch (ClassNotFoundException ex) {
      this.logger.error("Could not find class for query named " + className, ex);
    } catch (Exception ex) {
      this.logger.error("Could not execute query class for query " + className, ex);
    }

    return null;
  }

  @GetMapping("/api/query")
  public QuestionnaireResponseSimple getQuestionnaireResponse(@RequestParam(required = false) String overflowLocations, @RequestParam() String reportDate) {
    QuestionnaireResponseSimple response = new QuestionnaireResponseSimple();
    response.setDate(reportDate);

    Integer hospitalizedTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalized(), reportDate, overflowLocations);
    response.setHospitalized(hospitalizedTotal);

    Integer hospitalizedAndVentilatedTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalizedAndVentilated(), reportDate, overflowLocations);
    response.setHospitalizedAndVentilated(hospitalizedAndVentilatedTotal);

    Integer hospitalOnsetTotal = this.executeQueryCount(Config.getInstance().getQueryHospitalOnset(), reportDate, overflowLocations);
    response.setHospitalOnset(hospitalOnsetTotal);

    Integer edOverflowTotal = this.executeQueryCount(Config.getInstance().getQueryEDOverflow(), reportDate, overflowLocations);
    response.setEdOverflow(edOverflowTotal);

    Integer edOverflowAndVentilatedTotal = this.executeQueryCount(Config.getInstance().getQueryEDOverflowAndVentilated(), reportDate, overflowLocations);
    response.setEdOverflowAndVentilated(edOverflowAndVentilatedTotal);

    Integer deathsTotal = this.executeQueryCount(Config.getInstance().getQueryDeaths(), reportDate, overflowLocations);
    response.setDeaths(deathsTotal);

    return response;
  }
}
