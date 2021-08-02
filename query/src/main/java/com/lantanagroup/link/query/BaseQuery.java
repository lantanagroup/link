package com.lantanagroup.link.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class BaseQuery {
  protected ApplicationContext context;

  @Autowired
  protected QueryConfig queryConfig;

  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

  protected IGenericClient getFhirQueryClient() throws ClassNotFoundException {
    FhirContext ctx = FhirContext.forR4();
    ctx.getRestfulClientFactory().setSocketTimeout(30 * 1000);   // 30 seconds
    IGenericClient fhirQueryServer = ctx.newRestfulGenericClient(this.queryConfig.getFhirServerBase());

    if (Strings.isNotEmpty(this.queryConfig.getAuthClass())) {
      fhirQueryServer.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.queryConfig, this.context));
    }

    return fhirQueryServer;
  }
}
