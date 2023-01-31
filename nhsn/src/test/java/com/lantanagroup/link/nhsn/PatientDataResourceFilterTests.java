package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class PatientDataResourceFilterTests {
  /**
   * Condition
   * Uses onsetPeriod
   * Has neither start or end
   * Should remove because we can't determine and it would likely be excluded by measures anyways
   */
  @Test
  public void shouldRemove_Condition_PeriodNoStartNoEnd_Remove() {
    Period period = new Period();
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertTrue(actual);
  }

  /**
   * Condition
   * Uses onsetPeriod
   * Only has start
   * Start is 1/1/22 @ 5:40
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3)
   * should NOT remove
   */
  @Test
  public void shouldRemove_Condition_PeriodStart_DontRemove() {
    Period period = new Period().setStartElement(new DateTimeType("2022-01-01T05:40:00.000+00:00"));
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertFalse(actual);
  }

  /**
   * Condition
   * Uses onsetPeriod
   * Only has start
   * Start is 9/5/22 @ 5:40
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3/21)
   * SHOULD remove
   */
  @Test
  public void shouldRemove_Condition_PeriodStart_Remove() {
    Period period = new Period().setStartElement(new DateTimeType("2021-09-05T05:40:00.000+00:00"));
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertTrue(actual);
  }

  /**
   * Condition
   * Uses onsetPeriod
   * Only has end
   * End is 1/1/22 @ 5:40
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3)
   * should NOT remove
   */
  @Test
  public void shouldRemove_Condition_PeriodEnd_DontRemove() {
    Period period = new Period().setEndElement(new DateTimeType("2022-01-01T05:40:00.000+00:00"));
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertFalse(actual);
  }

  /**
   * Condition
   * Uses onsetPeriod
   * Only has end
   * End is 9/5/22 @ 5:40
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3/21)
   * SHOULD remove
   */
  @Test
  public void shouldRemove_Condition_PeriodEnd_Remove() {
    Period period = new Period().setEndElement(new DateTimeType("2021-09-05T05:40:00.000+00:00"));
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertTrue(actual);
  }

  /**
   * Condition
   * Uses onsetPeriod
   * Has both start ane end
   * 1/29/22 @ 5:40 - 2/3/22 @ 7:30
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3)
   * should NOT remove because period.start overlaps
   */
  @Test
  public void shouldRemove_Condition_PeriodStartAndEnd_DontRemove() {
    Period period = new Period()
            .setStartElement(new DateTimeType("2022-01-29T05:40:00.000+00:00"))
            .setEndElement(new DateTimeType("2022-02-03T07:30:00.000+00:00"));
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertFalse(actual);
  }

  /**
   * Condition
   * Uses onsetPeriod
   * Has both start ane end
   * 09/08/21 @ 5:40 - 09/12/21 @ 7:30
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3/21)
   * SHOULD remove
   */
  @Test
  public void shouldRemove_Condition_PeriodStartAndEnd_Remove() {
    Period period = new Period()
            .setStartElement(new DateTimeType("2021-09-08T05:40:00.000+00:00"))
            .setEndElement(new DateTimeType("2021-09-12T07:30:00.000+00:00"));
    Condition condition = new Condition();
    condition.setOnset(period);

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), condition);
    Assert.assertTrue(actual);
  }

  /**
   * ServiceRequest
   * Uses authoredOn of 1/8/22 @ 5:40
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3/21)
   * should NOT remove
   */
  @Test
  public void shouldRemove_ServiceRequest_AuthoredOn_DontRemove() {
    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setAuthoredOnElement(new DateTimeType("2022-01-08T05:40:00.000+00:00"));

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), serviceRequest);
    Assert.assertFalse(actual);
  }

  /**
   * ServiceRequest
   * Uses authoredOn of 9/3/21 @ 5:40
   * Report period is 1/1/22 - 1/31/22 with a 90d lookback (starting at 10/3/21)
   * SHOULD remove
   */
  @Test
  public void shouldRemove_ServiceRequest_AuthoredOn_Remove() {
    ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setAuthoredOnElement(new DateTimeType("2021-09-03T05:40:00.000+00:00"));

    ReportCriteria criteria = new ReportCriteria(new ArrayList<>(), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    boolean actual = PatientDataResourceFilter.shouldRemove(criteria, java.time.Period.ofDays(90), serviceRequest);
    Assert.assertTrue(actual);
  }
}
