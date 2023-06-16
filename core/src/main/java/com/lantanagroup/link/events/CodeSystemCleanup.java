package com.lantanagroup.link.events;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirScanner;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Finds references to invalid code systems and replaces them with valid code systems
 */
public class CodeSystemCleanup implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(CodeSystemCleanup.class);

  private final HashMap<String, String> codeSystemMap = new HashMap<>();

  public CodeSystemCleanup() {
    this.codeSystemMap.put("http://hl7.org/fhir/sid/icd-9-cm/diagnosis", "http://terminology.hl7.org/CodeSystem/icd9cm");
    this.codeSystemMap.put("http://hl7.org/fhir/v3/ParticipationType", "http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
  }

  private int checkCoding(Coding coding) {
    int fixCount = 0;
    if (coding.getSystem() != null && this.codeSystemMap.containsKey(coding.getSystem())) {
      String originalValue = coding.getSystem();
      coding.setSystem(this.codeSystemMap.get(originalValue));
      coding.addExtension()
              .setUrl(Constants.OriginalElementValueExtension)
              .setValue(new UriType(originalValue));

      fixCount++;
    }
    return fixCount;
  }

  @Override
  public void execute(TenantService tenantService, Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    int fixCount = 0;
    List<CodeableConcept> codeableConcepts = FhirScanner.findCodeableConcepts(data);
    for (CodeableConcept codeableConcept : codeableConcepts) {
      for (Coding coding : codeableConcept.getCoding()) {
        fixCount += checkCoding(coding);
      }
    }

    List<Coding> codings = FhirScanner.findCodings(data);
    for (Coding coding : codings) {
      fixCount += checkCoding(coding);
    }

    if (fixCount > 0) {
      logger.info("Fixed {} code systems", fixCount);
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    int fixCount = 0;

    for (DomainResource next : data) {
      List<CodeableConcept> codeableConcepts = FhirScanner.findCodeableConcepts(next);
      for (CodeableConcept codeableConcept : codeableConcepts) {
        for (Coding coding : codeableConcept.getCoding()) {
          fixCount += checkCoding(coding);
        }
      }

      List<Coding> codings = FhirScanner.findCodings(next);
      for (Coding coding : codings) {
        fixCount += checkCoding(coding);
      }
    }

    if (fixCount > 0) {
      logger.info("Fixed {} code systems", fixCount);
    }
  }
}
