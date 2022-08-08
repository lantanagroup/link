package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiFacilityConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

import static org.mockito.Mockito.*;

public class GenericAggregatorTests {
  @Test
  public void testGenerate() throws ParseException {
    GenericAggregator aggregator = mock(GenericAggregator.class);
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);

    when(aggregator.generate(any(), any())).thenCallRealMethod();
    doCallRealMethod().when(aggregator).setConfig(any());

    ApiConfig config = new ApiConfig();
    config.setFacility(new ApiFacilityConfig());
    config.getFacility().setName("Test Org");
    config.getFacility().setSystem("http://cdc.gov");
    config.getFacility().setValue("test-org-id");
    aggregator.setConfig(config);

    ReportCriteria criteria = new ReportCriteria("test", "2022-07-01T00:00:00Z", "2022-07-31T23:59:59Z");
    ReportContext context = new ReportContext(fhirDataProvider);

    MeasureReport report = aggregator.generate(criteria, context);

    Assert.assertNotNull(report);
    Assert.assertNotNull(report.getSubject());
    Assert.assertEquals("Test Org", report.getSubject().getDisplay());
    Assert.assertNotNull(report.getSubject().getIdentifier());
    Assert.assertEquals("http://cdc.gov", report.getSubject().getIdentifier().getSystem());
    Assert.assertEquals("test-org-id", report.getSubject().getIdentifier().getValue());
  }
}
