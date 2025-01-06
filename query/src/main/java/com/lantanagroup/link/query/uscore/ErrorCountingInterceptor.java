package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.link.ReportFailureException;
import com.lantanagroup.link.db.TenantService;
import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicInteger;

@Interceptor
public class ErrorCountingInterceptor {
  private final int maxConsecutiveErrors;
  private final AtomicInteger consecutiveErrors = new AtomicInteger();

  public ErrorCountingInterceptor(TenantService tenantService) {
    maxConsecutiveErrors = tenantService.getConfig().getFhirQuery().getMaxConsecutiveErrors();
  }

  @Hook(Pointcut.CLIENT_RESPONSE)
  public void interceptResponse(IHttpResponse response) {
    HttpStatus status = HttpStatus.resolve(response.getStatus());
    if (status != null && status.isError()) {
      int count = consecutiveErrors.incrementAndGet();
      if (count > maxConsecutiveErrors) {
        throw new ReportFailureException(String.format("Encountered %d consecutive query errors", count));
      }
    } else {
      consecutiveErrors.set(0);
    }
  }
}
