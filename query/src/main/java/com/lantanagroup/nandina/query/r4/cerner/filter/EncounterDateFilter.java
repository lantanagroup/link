package com.lantanagroup.nandina.query.r4.cerner.filter;

import com.lantanagroup.nandina.query.r4.cerner.PatientData;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class EncounterDateFilter extends Filter {

  private Date date;
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

  public EncounterDateFilter(Date date) {
    this.date = date;
  }

  public EncounterDateFilter(String dateStr) throws ParseException {
    this.date = sdf.parse(dateStr);
  }

  public Date getDate() {
    return date;
  }

  public String getDateAsString() {
    return sdf.format(date);
  }


  @Override
  public boolean runFilter(PatientData pd) {
    return hasEncounterDuringDate(pd);
  }

  private boolean hasEncounterDuringDate(PatientData pd) {
    boolean b = false;
    for (IBaseResource res : bundleToSet(pd.getEncounters())) {
      Encounter enc = (Encounter) res;
      Period p = enc.getPeriod();
      Date start = p.getStart();
      Date end = p.getEnd();
      if (start.before(date) && end.after(date)) b = true;
    }
    return b;
  }

}
