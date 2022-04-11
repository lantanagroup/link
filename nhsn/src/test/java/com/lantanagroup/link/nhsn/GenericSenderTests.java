package com.lantanagroup.link.nhsn;

public class GenericSenderTests {


//  @Test
//  public void sendContentTest() throws Exception {
//    HttpClient mockHttpClient = mock(HttpClient.class);
//    when(mockHttpClient.execute(any())).thenReturn(mock(HttpResponse.class));
//    HttpResponse httpResponse = mock(HttpResponse.class);
//    StatusLine httpResponseStatus = mock(StatusLine.class);
//    when(httpResponse.getStatusLine()).thenReturn(httpResponseStatus);
//    when(httpResponseStatus.getStatusCode()).thenReturn(200);
//
//    Header locationHeader = mock(Header.class);
//    Header[] headers = new Header[]{locationHeader};
//    HeaderElement headerElement = mock(HeaderElement.class);
//    HeaderElement[] headerElements = new HeaderElement[]{headerElement};
//    when(httpResponse.getHeaders("Location")).thenReturn(headers);
//    when(headers[0].getElements()).thenReturn(headerElements);
//    when(headerElement.getName()).thenReturn("www.testLocation.com/_history/");
//
//    // Create a config that has one URL in it to send to
//    FHIRSenderConfig config = new FHIRSenderConfig();
//   // config.setSendUrls(List.of("http://test.com/fhir/Bundle"));
//    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
//    FHIRSender mockSender = this.getMockSender(config);
//
////    config.setSendUrls(List.of("http://test.com/fhir/Bundle"));
////    config.setOAuthConfig(new FHIRSenderOAuthConfig());
////
////    config.getOAuthConfig().setCredentialMode(OAuthCredentialModes.Client);
////    config.getOAuthConfig().setTokenUrl("http://test.com/auth");
////    config.getOAuthConfig().setUsername("some-user");
////    config.getOAuthConfig().setPassword("some-pass");
////    config.getOAuthConfig().setScope("scope1 scope2 scope3");
////    HttpEntity mockAuthEntity = mock(HttpEntity.class);
//    InputStream mockAuthEntityIS = new ByteArrayInputStream("{\"access_token\": \"test-access-token\"}".getBytes());
//    HttpResponse mockAuthResponse = mock(HttpResponse.class);
//    when(mockAuthResponse.getEntity()).thenReturn(mockAuthEntity);
//    when(mockAuthEntity.getContent()).thenReturn(mockAuthEntityIS);
//    when(mockHttpClient.execute(any())).thenReturn(mockAuthResponse).thenReturn(httpResponse);
//
//
//    when(mockSender.getHttpClient()).thenReturn(mockHttpClient);
//    String xml = "<Bundle xmlns='http://hl7.org/fhir'><meta><tag><system value='https://nhsnlink.org'/><code value='report-bundle'/></tag></meta><type value='collection'/><entry><resource><MeasureReport xmlns='http://hl7.org/fhir'><evaluatedResource><reference value='Patient/testPatient1'/></evaluatedResource><evaluatedResource><reference value='Condition/testCondition1'/></evaluatedResource></MeasureReport></resource></entry></Bundle>";
//
//    String location = mockSender.sendContent(xml, "application/xml");
//    verify(mockHttpClient, times(2)).execute(any());
//    Assert.assertEquals(location, "www.testLocation.com");
//  }
//
//
//  @Test
//  public void updateDocumentLocation() throws Exception {
//    FHIRSenderConfig config = new FHIRSenderConfig();
//    config.setSendUrls(List.of("http://test.com/fhir/Bundle"));
//    IGenericClient mockFhirStoreClient = mock(IGenericClient.class);
//    FHIRSender mockSender = this.getMockSender(config);
//
//    DocumentReference documentReference = new DocumentReference();
//    FhirDataProvider mockFhirDataProvider = mock(FhirDataProvider.class);
//    when(mockFhirDataProvider.findDocRefForReport(anyString())).thenReturn(documentReference);
//
//    Attachment attachment = new Attachment();
//    DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReference.DocumentReferenceContentComponent();
//    documentReferenceContentComponent.setAttachment(attachment);
//    List<DocumentReference.DocumentReferenceContentComponent> referenceContentComponents = new ArrayList<>();
//    referenceContentComponents.add(documentReferenceContentComponent);
//    documentReference.setContent(referenceContentComponents);
//    // Create a MeasureReport for our test
//    MeasureReport measureReport = getMasterMeasureReport();
//    mockSender.updateDocumentLocation(measureReport, mockFhirDataProvider, "www.testLocation.com");
//    Assert.assertEquals(documentReference.getContent().get(0).getAttachment().getUrl(), "www.testLocation.com");
//  }
//
//
//  private FHIRSender getMockSender(FHIRSenderConfig config) throws Exception {
//    FHIRSender sender = mock(FHIRSender.class);
//
//    // Use Mockito for the FHIRSender because we need to mock the getHttpClient method
//    doCallRealMethod().when(sender).setConfig(any());
//    doCallRealMethod().when(sender).sendContent(any(), any());
//    doCallRealMethod().when(sender).updateDocumentLocation(any(), any(), any());
//    sender.setConfig(config);
//
//    return sender;
//  }
//
//  private MeasureReport getMasterMeasureReport() throws IOException {
//    String measureReportJson = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("fhir-sender-master-measure-report.json"));
//    FhirContext ctx = FhirContext.forR4();
//    return ctx.newJsonParser().parseResource(MeasureReport.class, measureReportJson);
//  }

}

