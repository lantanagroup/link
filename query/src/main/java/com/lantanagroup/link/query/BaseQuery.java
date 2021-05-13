package com.lantanagroup.link.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.ApplicationContext;

public class BaseQuery {
  protected ApplicationContext context;
  protected QueryConfig config;

  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

  public void setConfig(QueryConfig config) {
    this.config = config;
  }

  protected IGenericClient getFhirQueryClient() throws ClassNotFoundException {
    FhirContext ctx = FhirContext.forR4();
    IGenericClient fhirQueryServer = ctx.newRestfulGenericClient(this.config.getFhirServerBase());

    if (Strings.isNotEmpty(this.config.getAuthClass())) {
      fhirQueryServer.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.config, this.context));
    }

    return fhirQueryServer;
  }
}
