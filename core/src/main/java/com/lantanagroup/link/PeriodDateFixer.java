package com.lantanagroup.link;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateTimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

public class PeriodDateFixer
{
  private Bundle _bundle;
  private Logger _logger = LoggerFactory.getLogger(PeriodDateFixer.class);

  public PeriodDateFixer(Bundle bundle) {
    _bundle = bundle;
  }

  public void FixDates()
  {
    var periodsInBundle = FhirScanner.findPeriods(_bundle);

    periodsInBundle.forEach(p -> {

      //Fix the Start Date
      var start = p.getStart();
      start = FixDate(start);

      //Fix the End Date
      var end = p.getEnd();
      end = FixDate(end);

      var tz = TimeZone.getTimeZone(ZoneId.ofOffset("", ZoneOffset.UTC));

      //Set the fixed datetimes back into the Period, which should always include a time component if one was missing.
      p.setStartElement(new DateTimeType(start, TemporalPrecisionEnum.SECOND, tz));
      p.setEndElement(new DateTimeType(end, TemporalPrecisionEnum.SECOND, tz));

    });
  }

  private Date FixDate(Date date)
  {
    //Convert the deprecated Date object to the modern LocalDateTime, setting the timezone to UTC
    ZonedDateTime zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);

    //convert it back into a java.util.Date object
    var newDT = new java.util.Date(zdt.getYear()-1900, zdt.getMonthValue()-1, zdt.getDayOfMonth(), zdt.getHour(), zdt.getMinute(), zdt.getSecond());

    return newDT;
  }
}
