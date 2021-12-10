package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.lantanagroup.link.auth.OAuth2Helper;

import java.util.ArrayList;
import java.util.List;

public class AuthInterceptor extends AuthorizationInterceptor {

  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
    return new RuleBuilder()
            .allow().read().resourcesOfType("MeasureReport").withAnyId().andThen()
            .allow().write().resourcesOfType("MeasureReport").withAnyId().andThen()
            .allow().read().resourcesOfType("Bundle").withAnyId().andThen()
            .allow().write().resourcesOfType("Bundle").withAnyId().build();
  }
}
