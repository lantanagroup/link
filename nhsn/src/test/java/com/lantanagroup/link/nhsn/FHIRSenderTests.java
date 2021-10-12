package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.ITransaction;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.mock.TransactionMock;
import lombok.SneakyThrows;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.*;

public class FHIRSenderTests {
  @Test
  public void sendTest() throws Exception {
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    AuthMockInfo authMockInfo = MockHelper.mockAuth(fhirStoreClient);

    // Create a MeasureReport for our test
    MeasureReport measureReport = new MeasureReport();
    measureReport.addEvaluatedResource().setReference("Patient/testPatient1");
    measureReport.addEvaluatedResource().setReference("Condition/testCondition1");

    // Use Mockito for the FHIRSender because we need to mock the getHttpClient method
    FHIRSender sender = mock(FHIRSender.class);
    doCallRealMethod().when(sender).setConfig(any());
    doCallRealMethod().when(sender).send(any(), any(), any(), any(), any());

    // Create a config that has one URL in it to send to
    FHIRSenderConfig config = new FHIRSenderConfig();
    config.setSendUrls(List.of("http://test.com/fhir/Bundle"));
    sender.setConfig(config);

    // Mock the transaction that is used to get all resources for the report
    ITransaction transaction = mock(ITransaction.class);
    TransactionMock transactionMock = new TransactionMock(null, new Bundle());
    when(fhirStoreClient.transaction()).thenReturn(transaction);
    MockHelper.mockTransaction(transaction, transactionMock);

    // Mock the HttpClient that actually sends the HTTP POST request
    HttpClient mockHttpClient = mock(HttpClient.class);
    when(sender.getHttpClient()).thenReturn(mockHttpClient);
    when(mockHttpClient.execute(any())).thenReturn(mock(HttpResponse.class));

    // Mock the FHIR server's operation for POST AuditEvent
    ICreate create = mock(ICreate.class);
    when(fhirStoreClient.create()).thenReturn(create);
    MockHelper.mockAuditEvents(create);

    sender.send(measureReport, FhirContext.forR4(), request, authMockInfo.getAuthentication(), fhirStoreClient);

    // Make sure that a transaction was called to Bundle the resources for the report
    verify(transactionMock.getTransactionTyped(), times(1)).execute();

    // Make sure an HttpClient request was executed
    verify(mockHttpClient, times(1)).execute(argThat(new ArgumentMatcher<HttpUriRequest>() {
      @SneakyThrows
      @Override
      public boolean matches(HttpUriRequest httpUriRequest) {
        // Make sure the HTTP request includes the appropriate information, and is sent to the correct URL
        Assert.assertNotNull(httpUriRequest.getURI());
        Assert.assertEquals("http://test.com/fhir/Bundle", httpUriRequest.getURI().toString());
        Assert.assertTrue(httpUriRequest instanceof HttpEntityEnclosingRequest);
        HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpUriRequest;
        Assert.assertNotNull(httpEntityEnclosingRequest.getEntity());
        Assert.assertTrue(httpEntityEnclosingRequest.getEntity() instanceof StringEntity);
        String content = new String(httpEntityEnclosingRequest.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        Assert.assertEquals("<Bundle xmlns=\"http://hl7.org/fhir\"><meta><tag><system value=\"https://nhsnlink.org\"></system><code value=\"report-bundle\"></code></tag></meta><type value=\"collection\"></type><entry><resource><MeasureReport xmlns=\"http://hl7.org/fhir\"><evaluatedResource><reference value=\"Patient/testPatient1\"></reference></evaluatedResource><evaluatedResource><reference value=\"Condition/testCondition1\"></reference></evaluatedResource></MeasureReport></resource></entry></Bundle>", content);
        return true;
      }
  }
}
