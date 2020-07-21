package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.EncounterDateFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Report {

  protected static final Logger logger = LoggerFactory.getLogger(Report.class);
  protected List<PatientData> patientData;
  FhirContext ctx = FhirContext.forR4();
  IParser xmlParser = ctx.newXmlParser();

  public Report(EncounterScoop scoop, List<Filter> filters) {
    if (filters == null) filters = new ArrayList<Filter>();
    if (scoop.getReportDate() != null) {
      // TODO: Need to create date filters for Condition, Procedure, etc.
      // and add them here in addition to the EncounterDateFilter
      // or maybe create an "EverythingDate" filter that checks all that stuff.
      EncounterDateFilter edf = new EncounterDateFilter(scoop.getReportDate());
      filters.add(edf);
    }
    this.patientData = new ArrayList<PatientData>();
    for (PatientData pd : scoop.getPatientData()) {
      for (Filter filter : filters) {
        if (filter.runFilter(pd)) {
          patientData.add(pd);
          break;
        }
      }
    }
  }

  /**
   * @return a byte array with the report data. By default it is a FHIR Bundle of the report data after filtering. Override if you want something else like a Zip file containing CSVs
   * @throws IOException
   */
  public byte[] getReportData() throws IOException {
    Bundle b = getReportBundle();
    String bundleStr = xmlParser.encodeResourceToString(b);
    return bundleStr.getBytes();
  }

  /**
   * @return a FHIR Bundle resource containing the PatientData resources that are part of this report after every Filter has been run
   */
  public Bundle getReportBundle() {
    Bundle b = new Bundle();
    b.setType(BundleType.COLLECTION);
    for (PatientData pd : patientData) {
      b.addEntry().setResource(pd.getBundle());
    }
    return b;
  }

  public int getReportCount() {
    return patientData.size();
  }

}
