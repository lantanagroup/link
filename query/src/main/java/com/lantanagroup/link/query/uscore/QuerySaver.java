package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Interceptor
public class QuerySaver {
  private static final Logger logger = LoggerFactory.getLogger(QuerySaver.class);

  private final TenantService tenantService;
  private final String reportId;
  private final String queryType;

  public QuerySaver(TenantService tenantService, String reportId, String queryType) {
    this.tenantService = tenantService;
    this.reportId = reportId;
    this.queryType = queryType;
  }

  @Hook(Pointcut.CLIENT_REQUEST)
  public void onRequest(IHttpRequest request) {
    List<String> queryIds = request.getAllHeaders().get(Constants.HEADER_REQUEST_ID);
    if (queryIds == null || queryIds.isEmpty()) {
      return;
    }
    Query query = new Query();
    query.setId(UUID.fromString(queryIds.get(0)));
    query.setReportId(reportId);
    query.setQueryType(queryType);
    query.setUrl(request.getUri());
    try {
      query.setBody(request.getRequestBodyFromStream());
    } catch (IOException ignored) {
    }
    query.setRetrieved(new Date());
    tenantService.saveQuery(query);
  }
}
