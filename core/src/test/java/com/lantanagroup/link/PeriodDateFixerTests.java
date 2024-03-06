package com.lantanagroup.link;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class PeriodDateFixerTests {
  @Test
  public void findPeriodsBundleTest() {
    Bundle bundle = new Bundle();
    Encounter enc = new Encounter();
    enc.getPeriod().setStartElement(new DateTimeType("2023-05-25"));
    enc.getPeriod().setEndElement(new DateTimeType("2023-05-25T12:00:00Z"));
    bundle.addEntry().setResource(enc);

    List<Period> periods = FhirScanner.findPeriods(bundle);
    Assert.assertEquals(1, periods.size());
  }

  @Test
  public void fixDatesTest()
  {
    Bundle bundle = new Bundle();
    Encounter enc = new Encounter();

    var tz = TimeZone.getTimeZone(ZoneId.ofOffset("", ZoneOffset.UTC));
    enc.getPeriod().setStartElement(new DateTimeType(new Date(2023-1900, 5-1, 25), TemporalPrecisionEnum.DAY, tz));
    var startString = enc.getPeriod().getStartElement().asStringValue();

    enc.getPeriod().setEndElement(new DateTimeType("2023-05-25T12:00:00+00:00"));
    var endString = enc.getPeriod().getEndElement().asStringValue();

    bundle.addEntry().setResource(enc);

    var periods = FhirScanner.findPeriods(bundle);

    var fixer = new PeriodDateFixer(bundle);
    fixer.FixDates();

    var period = periods.get(0);
    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoFormat.setTimeZone(tz);
    var start = isoFormat.parse("2023-05-25T00:00:00Z", new ParsePosition(0));

    isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoFormat.setTimeZone(tz);
    var end = isoFormat.parse("2023-05-25T12:00:00Z", new ParsePosition(0));

    Assert.assertEquals(start, period.getStart());
    Assert.assertNotEquals(startString, period.getStartElement().asStringValue());

    Assert.assertTrue(period.getStartElement().hasExtension());
    Extension extension = period.getStartElement().getExtensionFirstRep();
    Assert.assertEquals(Constants.OriginalElementValueExtension, extension.getUrl());
    Assert.assertEquals("2023-05-25", extension.getValue().primitiveValue());

    Assert.assertEquals(end, period.getEnd());
    Assert.assertEquals(endString, period.getEndElement().asStringValue());
  }
}
