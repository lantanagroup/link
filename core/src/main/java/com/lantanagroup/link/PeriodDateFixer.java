package com.lantanagroup.link;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateTimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        DateTimeType start = p.getStartElement();
        DateTimeType end = p.getEndElement();

        if (start.asStringValue() != null && end.asStringValue() != null &&
                start.getPrecision() == TemporalPrecisionEnum.DAY &&
                end.getPrecision().getCalendarConstant() > TemporalPrecisionEnum.DAY.getCalendarConstant()) {
          p.getStartElement().addExtension()
                  .setUrl(Constants.OriginalElementValueExtension)
                  .setValue(p.getStartElement().copy());

          start.setPrecision(end.getPrecision());
          start.setTimeZone(end.getTimeZone());
          start.setHour(0);
          start.setMinute(0);
          start.setSecond(0);
          start.setMillis(0);
      }

    });
  }
}
