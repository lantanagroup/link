package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class GenericSenderTests {
  @Test
  public void sendContentTest() throws Exception {
    CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
    when(mockHttpClient.execute(any())).thenReturn(mock(CloseableHttpResponse.class));
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    StatusLine httpResponseStatus = mock(StatusLine.class);
    when(httpResponse.getStatusLine()).thenReturn(httpResponseStatus);
    when(httpResponseStatus.getStatusCode()).thenReturn(200);

    Header locationHeader = mock(Header.class);
    Header[] headers = new Header[]{locationHeader};
    HeaderElement headerElement = mock(HeaderElement.class);
    HeaderElement[] headerElements = new HeaderElement[]{headerElement};
    when(httpResponse.getHeaders("Location")).thenReturn(headers);
    when(headers[0].getElements()).thenReturn(headerElements);
    when(headerElement.getName()).thenReturn("www.testLocation.com/_history/");

    // Create a config that has one URL in it to send to
    FHIRSenderConfig config = new FHIRSenderConfig();
    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
    FHIRSender mockSender = this.getMockSender(config);

    config.setUrl("http://test.com/fhir");
    config.setAuthConfig(new LinkOAuthConfig());
    config.getAuthConfig().setCredentialMode("client");
    config.getAuthConfig().setTokenUrl("http://test.com/auth");
    config.getAuthConfig().setUsername("some-user");
    config.getAuthConfig().setPassword("some-pass");
    config.getAuthConfig().setScope("scope1 scope2 scope3");
    HttpEntity mockAuthEntity = mock(HttpEntity.class);
    InputStream mockAuthEntityIS = new ByteArrayInputStream("{\"access_token\": \"test-access-token\"}".getBytes());
    CloseableHttpResponse mockAuthResponse = mock(CloseableHttpResponse.class);
    when(mockAuthResponse.getEntity()).thenReturn(mockAuthEntity);
    when(mockAuthEntity.getContent()).thenReturn(mockAuthEntityIS);
    when(mockHttpClient.execute(any())).thenReturn(mockAuthResponse).thenReturn(httpResponse);

    when(mockSender.getHttpClient()).thenReturn(mockHttpClient);
    String xml = "<Bundle xmlns='http://hl7.org/fhir'><meta><tag><system value='https://nhsnlink.org'/><code value='report-bundle'/></tag></meta><type value='collection'/><entry><resource><MeasureReport xmlns='http://hl7.org/fhir'><evaluatedResource><reference value='Patient/testPatient1'/></evaluatedResource><evaluatedResource><reference value='Condition/testCondition1'/></evaluatedResource></MeasureReport></resource></entry></Bundle>";

    verify(mockHttpClient, times(0)).execute(any());
  }

  @Test
  public void updateDocumentLocation() throws Exception {
    FHIRSenderConfig config = new FHIRSenderConfig();
    config.setUrl("http://test.com/fhir");
    config.setAuthConfig(new LinkOAuthConfig());
    config.getAuthConfig().setCredentialMode("client");
    config.getAuthConfig().setTokenUrl("http://test.com/auth");
    config.getAuthConfig().setUsername("some-user");
    config.getAuthConfig().setPassword("some-pass");
    config.getAuthConfig().setScope("scope1 scope2 scope3");
    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
    FHIRSender mockSender = this.getMockSender(config);

    DocumentReference documentReference = new DocumentReference();
    FhirDataProvider mockFhirDataProvider = mock(FhirDataProvider.class);
    when(mockFhirDataProvider.findDocRefForReport(anyString())).thenReturn(documentReference);

    Attachment attachment = new Attachment();
    DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReference.DocumentReferenceContentComponent();
    documentReferenceContentComponent.setAttachment(attachment);
    List<DocumentReference.DocumentReferenceContentComponent> referenceContentComponents = new ArrayList<>();
    referenceContentComponents.add(documentReferenceContentComponent);
    documentReference.setContent(referenceContentComponents);
    // Create a MeasureReport for our test
    MeasureReport measureReport = getMasterMeasureReport();
    mockSender.updateDocumentLocation(measureReport, mockFhirDataProvider, "www.testLocation.com");
  }


  private FHIRSender getMockSender(FHIRSenderConfig config) throws Exception {
    FHIRSender sender = mock(FHIRSender.class);

    // Use Mockito for the FHIRSender because we need to mock the getHttpClient method
    doCallRealMethod().when(sender).setFhirSenderConfig(any());
    // doCallRealMethod().when(sender).sendContent(any(), any(), any(), any());
    doCallRealMethod().when(sender).updateDocumentLocation(any(), any(), any());
    sender.setFhirSenderConfig(config);

    return sender;
  }

  private MeasureReport getMasterMeasureReport() throws IOException {
    String measureReportJson = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("fhir-sender-master-measure-report.json"), Charset.defaultCharset());
    FhirContext ctx = FhirContextProvider.getFhirContext();
    return ctx.newJsonParser().parseResource(MeasureReport.class, measureReportJson);
  }

}

