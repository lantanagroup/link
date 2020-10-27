package com.lantanagroup.nandina.query.pillbox.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.BaseFormQuery;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PillboxCsvReport;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.ArrayList;
import java.util.List;

public class FormQuery extends BaseFormQuery {

  @Override
  public void execute() {
    EncounterScoop scoop = (EncounterScoop) this.getContextData("scoopData");
    FhirContext fhirContext = (FhirContext) this.getContextData("fhirContext");
    List<Filter> filters = new ArrayList<Filter>();

    PillboxCsvReport csvReport = new PillboxCsvReport(scoop, filters, fhirContext);

    this.setAnswer("patients", csvReport.getUniqueData());
    this.setAnswer("meds", csvReport.getMedsData());
    this.setAnswer("labs", csvReport.getLabData());
    this.setAnswer("dx", csvReport.getDxData());
  }
}
