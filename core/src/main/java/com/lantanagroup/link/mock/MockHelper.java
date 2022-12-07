package com.lantanagroup.link.mock;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.auth.LinkCredentials;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.mockito.ArgumentMatcher;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;

public class MockHelper {
  public static IQuery<IBaseBundle> mockSearchForResource(
          IUntypedQuery<IBaseBundle> untypedQuery,
          String resourceType,
          ArgumentMatcher<ICriterion<?>> argumentMatcher,
          Resource... resResources) {
    IQuery<IBaseBundle> subBundleIntQuery = mock(IQuery.class);
    IQuery<Bundle> subBundleQuery = mock(IQuery.class);

    Bundle responseBundle = new Bundle();

    if (resResources != null) {
      for (Resource resource : resResources) {
        responseBundle.addEntry().setResource(resource);
      }
    }

    when(untypedQuery.forResource(resourceType)).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.where(any(ICriterion.class))).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.returnBundle(Bundle.class)).thenReturn(subBundleQuery);
    when(subBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(subBundleQuery);
    when(subBundleQuery.execute()).thenReturn(responseBundle);

    return subBundleIntQuery;
  }

  public static IQuery<IBaseBundle> mockFailSearchForResource(
          IUntypedQuery<IBaseBundle> untypedQuery,
          String resourceType) {
    IQuery<IBaseBundle> subBundleIntQuery = mock(IQuery.class);
    IQuery<Bundle> subBundleQuery = mock(IQuery.class);

    Bundle responseBundle = new Bundle();

    when(untypedQuery.forResource(resourceType)).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.where(any(ICriterion.class))).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.returnBundle(Bundle.class)).thenReturn(subBundleQuery);
    when(subBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(subBundleQuery);
    when(subBundleQuery.execute()).thenReturn(responseBundle);

    return subBundleIntQuery;
  }

  public static AuthMockInfo mockAuth(IGenericClient client) {
    Authentication auth = mock(Authentication.class);
    LinkCredentials user = mock(LinkCredentials.class);
    DecodedJWT jwt = mock(DecodedJWT.class);

    AuthMockInfo authMock = new AuthMockInfo();
    authMock.setAuthentication(auth);
    authMock.setUser(user);

    when(user.getJwt()).thenReturn(jwt);
    when(jwt.getPayload()).thenReturn("ewogICJzdWIiOiAiMTIzNDU2Nzg5MCIsCiAgIm5hbWUiOiAiSm9obiBEb2UiLAogICJpYXQiOiAxNTE2MjM5MDIyCn0=");
    when(auth.getPrincipal()).thenReturn(user);

    return authMock;
  }

  public static void mockTransaction(ITransaction transaction, TransactionMock ...transactionMocks) {
    for (TransactionMock transactionMock : transactionMocks) {
      ITransactionTyped<Bundle> transactionTyped = mock(ITransactionTyped.class);
      transactionMock.setTransactionTyped(transactionTyped);

      if (transactionMock.getArgumentMatcher() != null) {
        when(transaction.withBundle(argThat(transactionMock.getArgumentMatcher()))).thenReturn(transactionTyped);
      } else {
        when(transaction.withBundle(any(Bundle.class))).thenReturn(transactionTyped);
      }

      when(transactionTyped.execute()).thenReturn(transactionMock.getResponseBundle());
    }
  }

  public static void mockResourceCreation(ICreate create, Resource resource){
    MethodOutcome createMethod = new MethodOutcome();
    IIdType id = new IdType("OperationOutcome", "outcome1", "1");
    createMethod.setId(id);
    createMethod.setCreated(true);
    createMethod.setResource(resource);

    ICreateTyped createTyped = mock(ICreateTyped.class);
    when(create.resource(resource)).thenReturn(createTyped);
    when(createTyped.execute()).thenReturn(createMethod);
  }

  public static void mockResourceUpdate(IUpdate update, Resource resource){
    MethodOutcome outcome = new MethodOutcome();
    IIdType id = new IdType("OperationOutcome", "outcome1", "1");
    outcome.setId(id);
    outcome.setCreated(true);
    outcome.setResource(resource);

    IUpdateTyped updateTyped = mock(IUpdateTyped.class);
    when(update.resource(any(Resource.class))).thenReturn(updateTyped);
    when(updateTyped.execute()).thenReturn(outcome);
  }
  
  public static void mockAuditEvents(ICreate create) {
    MethodOutcome createMethod = new MethodOutcome();
    createMethod.setId(new IdType("test"));

    ICreateTyped createTyped = mock(ICreateTyped.class);

    when(create.resource(any(AuditEvent.class))).thenReturn(createTyped);
    when(createTyped.prettyPrint()).thenReturn(createTyped);
    when(createTyped.encodedJson()).thenReturn(createTyped);
    when(createTyped.execute()).thenReturn(createMethod);
  }

  @SuppressWarnings("rawtypes")
  public static void mockInstanceOperation(IGenericClient client, String opName, ArgumentMatcher<IIdType> onInstanceMatcher, ArgumentMatcher<Parameters> parametersMatcher, Object mockResponseObj) {
    IOperation operation = mock(IOperation.class);
    IOperationUnnamed operationUnnamed = mock(IOperationUnnamed.class);
    IOperationUntyped operationUntyped = mock(IOperationUntyped.class);
    IOperationUntypedWithInputAndPartialOutput operationUntypedWithInputAndPartialOutput = mock(IOperationUntypedWithInputAndPartialOutput.class);
    IOperationUntypedWithInput operationUntypedWithInput = mock(IOperationUntypedWithInput.class);

    when(client.operation()).thenReturn(operation);

    if (onInstanceMatcher != null) {
      when(operation.onInstance(argThat(onInstanceMatcher))).thenReturn(operationUnnamed);
    } else {
      when(operation.onInstance(any())).thenReturn(operationUnnamed);
    }

    when(operationUnnamed.named("$evaluate-measure")).thenReturn(operationUntyped);

    if (parametersMatcher != null) {
      when(operationUntyped.withParameters(argThat(parametersMatcher))).thenReturn(operationUntypedWithInputAndPartialOutput);
    } else {
      when(operationUntyped.withParameters(any())).thenReturn(operationUntypedWithInputAndPartialOutput);
    }

    when(operationUntypedWithInputAndPartialOutput.useHttpGet()).thenReturn(operationUntypedWithInput);
    when(operationUntypedWithInput.returnResourceType(MeasureReport.class)).thenReturn(operationUntypedWithInput);
    when(operationUntypedWithInput.execute()).thenReturn(mockResponseObj);
  }
}
