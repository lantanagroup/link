package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.OAuthCredentialModes;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FHIRSenderOAuthConfig;
import com.lantanagroup.link.config.sender.FhirSenderUrlOAuthConfig;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class GenericSenderTests {


  @Test
  public void sendContentTest() throws Exception {
    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.execute(any())).thenReturn(mock(HttpResponse.class));
    HttpResponse httpResponse = mock(HttpResponse.class);
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
    XMLSender mockSender = this.getMockSender(config);

    FhirSenderUrlOAuthConfig urlAuth = new FhirSenderUrlOAuthConfig();
    config.setSendUrls(List.of(urlAuth));
    config.getSendUrls().get(0).setUrl("http://test.com/fhir/Bundle");
    config.getSendUrls().get(0).setAuthConfig(new FHIRSenderOAuthConfig());
    config.getSendUrls().get(0).getAuthConfig().setCredentialMode(OAuthCredentialModes.Client);
    config.getSendUrls().get(0).getAuthConfig().setTokenUrl("http://test.com/auth");
    config.getSendUrls().get(0).getAuthConfig().setUsername("some-user");
    config.getSendUrls().get(0).getAuthConfig().setPassword("some-pass");
    config.getSendUrls().get(0).getAuthConfig().setScope("scope1 scope2 scope3");
    HttpEntity mockAuthEntity = mock(HttpEntity.class);
    InputStream mockAuthEntityIS = new ByteArrayInputStream("{\"access_token\": \"test-access-token\"}".getBytes());
    HttpResponse mockAuthResponse = mock(HttpResponse.class);
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
    FhirSenderUrlOAuthConfig urlAuth = new FhirSenderUrlOAuthConfig();
    urlAuth.setUrl("http://test.com/fhir/Bundle");
    config.setSendUrls(List.of(urlAuth));
    config.getSendUrls().get(0).setAuthConfig(new FHIRSenderOAuthConfig());
    config.getSendUrls().get(0).getAuthConfig().setCredentialMode(OAuthCredentialModes.Client);
    config.getSendUrls().get(0).getAuthConfig().setTokenUrl("http://test.com/auth");
    config.getSendUrls().get(0).getAuthConfig().setUsername("some-user");
    config.getSendUrls().get(0).getAuthConfig().setPassword("some-pass");
    config.getSendUrls().get(0).getAuthConfig().setScope("scope1 scope2 scope3");
    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
    XMLSender mockSender = this.getMockSender(config);

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


  private XMLSender getMockSender(FHIRSenderConfig config) throws Exception {
    XMLSender sender = mock(XMLSender.class);

    // Use Mockito for the FHIRSender because we need to mock the getHttpClient method
    doCallRealMethod().when(sender).setConfig(any());
    // doCallRealMethod().when(sender).sendContent(any(), any(), any(), any());
    doCallRealMethod().when(sender).updateDocumentLocation(any(), any(), any());
    sender.setConfig(config);

    return sender;
  }

  private MeasureReport getMasterMeasureReport() throws IOException {
    String measureReportJson = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("fhir-sender-master-measure-report.json"));
    FhirContext ctx = FhirContextProvider.getFhirContext();
    return ctx.newJsonParser().parseResource(MeasureReport.class, measureReportJson);
  }

}

