package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ITransaction;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.OAuthCredentialModes;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FHIRSenderOAuthConfig;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.mock.TransactionMock;
import lombok.SneakyThrows;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class FHIRSenderTests {
  private void runTest(FHIRSender mockSender, IGenericClient mockFhirStoreClient, HttpClient mockHttpClient, MeasureReport measureReport, ArgumentMatcher<HttpUriRequest> httpArgMatcher) throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    AuthMockInfo authMockInfo = MockHelper.mockAuth(mockFhirStoreClient);
    FhirDataProvider mockFhirDataProvider = mock(FhirDataProvider.class);
    // Mock the transaction that is used to get all resources for the report
    ITransaction transaction = mock(ITransaction.class);
    TransactionMock transactionMock = new TransactionMock(null, new Bundle());
    when(mockFhirStoreClient.transaction()).thenReturn(transaction);
    MockHelper.mockTransaction(transaction, transactionMock);

    // Mock the HttpClient that actually sends the HTTP POST request
    when(mockFhirDataProvider.getClient()).thenReturn(mockFhirStoreClient);
    String xml = "<Bundle xmlns='http://hl7.org/fhir'><meta><tag><system value='https://nhsnlink.org'/><code value='report-bundle'/></tag></meta><type value='collection'/><entry><resource><MeasureReport xmlns='http://hl7.org/fhir'><evaluatedResource><reference value='Patient/testPatient1'/></evaluatedResource><evaluatedResource><reference value='Condition/testCondition1'/></evaluatedResource></MeasureReport></resource></entry></Bundle>";
    when(mockFhirDataProvider.bundleToXml(any(Bundle.class))).thenReturn(xml);
    FhirContext ctx = FhirContext.forR4();
    when(mockFhirStoreClient.getFhirContext()).thenReturn(ctx);
    when(mockSender.getHttpClient()).thenReturn(mockHttpClient);


    // Mock the FHIR server's operation for POST AuditEvent
    ICreate create = mock(ICreate.class);
    when(mockFhirStoreClient.create()).thenReturn(create);
    MockHelper.mockAuditEvents(create);
    when(mockFhirDataProvider.transaction(any(Bundle.class))).thenReturn(new Bundle());
    mockSender.send(measureReport, request, authMockInfo.getAuthentication(), mockFhirDataProvider, true);

    // Make sure an HttpClient request was executed
    verify(mockHttpClient, times(1)).execute(argThat(httpArgMatcher));
  }

  private FHIRSender getMockSender(FHIRSenderConfig config) throws Exception {
    FHIRSender sender = mock(FHIRSender.class);

    // Use Mockito for the FHIRSender because we need to mock the getHttpClient method
    doCallRealMethod().when(sender).setConfig(any());
    doCallRealMethod().when(sender).send(any(), any(), any(), any(), anyBoolean());

    sender.setConfig(config);

    return sender;
  }


  @Test
  public void sendTestUnauthenticated() throws Exception {

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.execute(any())).thenReturn(mock(HttpResponse.class));

    // Create a config that has one URL in it to send to
    FHIRSenderConfig config = new FHIRSenderConfig();
    config.setSendUrls(List.of("http://test.com/fhir/Bundle"));

    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
    FHIRSender mockSender = this.getMockSender(config);

    // Create a MeasureReport for our test
    MeasureReport measureReport = new MeasureReport();
    measureReport.addEvaluatedResource().setReference("Patient/testPatient1");
    measureReport.addEvaluatedResource().setReference("Condition/testCondition1");

    ArgumentMatcher<HttpUriRequest> httpUriRequestArgumentMatcher = new ArgumentMatcher<HttpUriRequest>() {
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
        Bundle requestBundle = (Bundle) FhirContext.forR4().newXmlParser().parseResource(content);
        Assert.assertNotNull(requestBundle);
        Assert.assertEquals(httpEntityEnclosingRequest.getHeaders("Authorization").length, 0);
        return true;
      }
    };

    // Mock the response of the HttpClient
    HttpResponse httpResponse = mock(HttpResponse.class);
    StatusLine httpResponseStatus = mock(StatusLine.class);
    when(mockHttpClient.execute(any())).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(httpResponseStatus);
    when(httpResponseStatus.getStatusCode()).thenReturn(201);

    this.runTest(mockSender, mockFhirStoreClient, mockHttpClient, measureReport, httpUriRequestArgumentMatcher);
  }


  @Test
  public void sendTestAuthenticated() throws Exception {
    // Mock the response of the HttpClient
    HttpResponse httpResponse = mock(HttpResponse.class);
    StatusLine httpResponseStatus = mock(StatusLine.class);
    when(httpResponse.getStatusLine()).thenReturn(httpResponseStatus);
    when(httpResponseStatus.getStatusCode()).thenReturn(201);

    HttpClient mockHttpClient = mock(HttpClient.class);

    when(mockHttpClient.execute(any())).thenReturn(httpResponse);

    HttpResponse mockAuthResponse = mock(HttpResponse.class);
    HttpEntity mockAuthEntity = mock(HttpEntity.class);
    InputStream mockAuthEntityIS = new ByteArrayInputStream("{\"access_token\": \"test-access-token\"}".getBytes());
    when(mockAuthResponse.getEntity()).thenReturn(mockAuthEntity);
    when(mockAuthEntity.getContent()).thenReturn(mockAuthEntityIS);

    when(mockHttpClient.execute(any()))
            .thenReturn(mockAuthResponse)     // First call to execute HTTP request
            .thenReturn(httpResponse);
    // Second call to execute

    // Create a config that has one URL in it to send to
    FHIRSenderConfig config = new FHIRSenderConfig();
    config.setSendUrls(List.of("http://test.com/fhir/Bundle"));
    config.setOAuthConfig(new FHIRSenderOAuthConfig());

    config.getOAuthConfig().setCredentialMode(OAuthCredentialModes.Client);
    config.getOAuthConfig().setTokenUrl("http://test.com/auth");
    config.getOAuthConfig().setUsername("some-user");
    config.getOAuthConfig().setPassword("some-pass");
    config.getOAuthConfig().setScope("scope1 scope2 scope3");

    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
    FHIRSender mockSender = this.getMockSender(config);

    // Create a MeasureReport for our test
    MeasureReport measureReport = new MeasureReport();
    measureReport.addEvaluatedResource().setReference("Patient/testPatient1");
    measureReport.addEvaluatedResource().setReference("Condition/testCondition1");

    ArgumentMatcher<HttpUriRequest> httpUriRequestArgumentMatcher = new ArgumentMatcher<HttpUriRequest>() {
      @SneakyThrows
      @Override
      public boolean matches(HttpUriRequest httpUriRequest) {
        // Make sure the HTTP request includes the appropriate information, and is sent to the correct URL
        Assert.assertNotNull(httpUriRequest.getURI());

        if (!httpUriRequest.getURI().toString().equals("http://test.com/fhir/Bundle")) {
          return false;
        }

        Assert.assertTrue(httpUriRequest instanceof HttpEntityEnclosingRequest);
        HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpUriRequest;
        Assert.assertNotNull(httpEntityEnclosingRequest.getEntity());
        Assert.assertTrue(httpEntityEnclosingRequest.getEntity() instanceof StringEntity);
        String content = new String(httpEntityEnclosingRequest.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        Bundle requestBundle = (Bundle) FhirContext.forR4().newXmlParser().parseResource(content);
        Assert.assertNotNull(requestBundle);
        Header[] headers = httpEntityEnclosingRequest.getHeaders("Authorization");
        Assert.assertEquals(headers.length, 1);
        Boolean found = Arrays.stream(headers).anyMatch(h -> h.getValue().equals("Bearer test-access-token"));
        Assert.assertEquals(true, found);
        return true;
      }
    };

    this.runTest(mockSender, mockFhirStoreClient, mockHttpClient, measureReport, httpUriRequestArgumentMatcher);
  }
}
