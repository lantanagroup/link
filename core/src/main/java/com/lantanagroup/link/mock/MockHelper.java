package com.lantanagroup.link.mock;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.auth.LinkCredentials;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.mockito.ArgumentMatcher;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;

public class MockHelper {
  public static AuthMockInfo mockAudit(IGenericClient client) {
    Authentication auth = mock(Authentication.class);
    LinkCredentials user = mock(LinkCredentials.class);
    DecodedJWT jwt = mock(DecodedJWT.class);

    AuthMockInfo authMock = new AuthMockInfo();
    authMock.setAuthentication(auth);
    authMock.setUser(user);

    when(user.getJwt()).thenReturn(jwt);
    when(jwt.getPayload()).thenReturn("ewogICJzdWIiOiAiMTIzNDU2Nzg5MCIsCiAgIm5hbWUiOiAiSm9obiBEb2UiLAogICJpYXQiOiAxNTE2MjM5MDIyCn0=");

    return authMock;
  }

  public static void mockTransaction(ITransaction transaction, TransactionMock ...transactionMocks) {
    for (TransactionMock transactionMock : transactionMocks) {
      ITransactionTyped<Bundle> transactionTyped = mock(ITransactionTyped.class);
      when(transaction.withBundle(argThat(transactionMock.getArgumentMatcher()))).thenReturn(transactionTyped);
      when(transactionTyped.execute()).thenReturn(transactionMock.getResponseBundle());
    }
  }

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
