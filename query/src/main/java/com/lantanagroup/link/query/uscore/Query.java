package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.QueryPhase;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class Query {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  @Setter
  private IGenericClient fhirQueryClient;

  @Setter
  private ApplicationContext applicationContext;

  public IGenericClient getFhirQueryClient(TenantService tenantService, String reportId) {
    if (this.fhirQueryClient != null) {
      return this.fhirQueryClient;
    }

    //this.getFhirContext().getRestfulClientFactory().setSocketTimeout(30 * 1000);   // 30 seconds
    IGenericClient fhirQueryClient = FhirContextProvider.getFhirContext()
            .newRestfulGenericClient(tenantService.getConfig().getFhirQuery().getFhirServerBase());

    fhirQueryClient.registerInterceptor(new LoggingInterceptor());
    fhirQueryClient.registerInterceptor(new QuerySaver(tenantService, reportId, "FhirQuery"));

    if (StringUtils.isNotEmpty(tenantService.getConfig().getFhirQuery().getAuthClass())) {
      logger.debug(String.format("Authenticating queries using %s", tenantService.getConfig().getFhirQuery().getAuthClass()));

      try {
        fhirQueryClient.registerInterceptor(new HapiFhirAuthenticationInterceptor(tenantService, this.applicationContext));
      } catch (ClassNotFoundException e) {
        logger.error("Error registering authentication interceptor", e);
      }
    } else {
      logger.warn("No authentication is configured for the FHIR server being queried");
    }

    fhirQueryClient.forceConformanceCheck();

    this.fhirQueryClient = fhirQueryClient;
    return fhirQueryClient;
  }

  public void execute(TenantService tenantService, ReportCriteria criteria, ReportContext context, QueryPhase queryPhase) {
    List<PatientOfInterestModel> patientsOfInterest = context.getPatientsOfInterest(queryPhase);
    if (patientsOfInterest.size() > 0) {
      try {
        PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
        scoop.setFhirQueryServer(this.getFhirQueryClient(tenantService, context.getMasterIdentifierValue()));
        scoop.setTenantService(tenantService);
        scoop.execute(criteria, context, patientsOfInterest, queryPhase);
      } catch (Exception ex) {
        logger.error("Error scooping data for patients", ex);
      }
    }
  }
}
