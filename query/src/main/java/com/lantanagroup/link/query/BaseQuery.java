package com.lantanagroup.link.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class BaseQuery {
  private static final Logger logger = LoggerFactory.getLogger(BaseQuery.class);

  @Setter
  protected ApplicationContext applicationContext;

  @Setter
  private FhirContext fhirContext;

  @Setter
  private IGenericClient fhirQueryClient;

  @Autowired
  protected QueryConfig queryConfig;

  public FhirContext getFhirContext() {
    if (this.fhirContext != null) {
      return this.fhirContext;
    }

    this.fhirContext = FhirContextProvider.getFhirContext();
    return this.fhirContext;
  }

  protected IGenericClient getFhirQueryClient() throws ClassNotFoundException {
    if (this.fhirQueryClient != null) {
      return this.fhirQueryClient;
    }

    //this.getFhirContext().getRestfulClientFactory().setSocketTimeout(30 * 1000);   // 30 seconds
    IGenericClient fhirQueryClient = this.getFhirContext().newRestfulGenericClient(this.queryConfig.getFhirServerBase());

    /*
    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLogRequestSummary(true);
    loggingInterceptor.setLogRequestBody(true);
    fhirQueryClient.registerInterceptor(loggingInterceptor);
     */

    if (Strings.isNotEmpty(this.queryConfig.getAuthClass())) {
      logger.debug(String.format("Authenticating queries using %s", this.queryConfig.getAuthClass()));
      fhirQueryClient.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.queryConfig, this.applicationContext));
    } else {
      logger.warn("No authentication is configured for the FHIR server being queried");
    }

    this.fhirQueryClient = fhirQueryClient;
    return fhirQueryClient;
  }
}
