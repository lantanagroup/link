package com.lantanagroup.nandina.query.r4.cerner.filter;

import com.lantanagroup.nandina.query.r4.cerner.PatientData;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;

import java.util.Set;

public final class CovidFilter extends Filter {


  public boolean runFilter(PatientData pd) {
    boolean b = false;
    if (
            hasCovidCondition(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1124"))
                    || hasCovidCondition(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1203"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1142"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1144"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1152"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1153"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1154"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1157"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1158"))
                    || hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1223"))
                    || hasCovidLabResult(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1143"))
                    || hasCovidLabResult(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1155"))
    ) {
      b = true;
    }
    ;
    return b;
  }


  private boolean hasCovidLabTest(PatientData pd, Set<String> codeSet) {
    boolean b = false;
    for (IBaseResource res : bundleToSet(pd.getLabResults())) {
      Observation c = (Observation) res;
      CodeableConcept cc = c.getCode();
      b = codeInSet(cc, codeSet);
      if (b) {
        //	logger.info("Patient has covid: " + pd.patient.getId());
        //	logger.info(" - " + cc.getCodingFirstRep().getCode());
        break;
      }
    }
    return b;
  }

  private boolean hasCovidLabResult(PatientData pd, Set<String> codeSet) {
    boolean b = false;
    for (IBaseResource res : bundleToSet(pd.getLabResults())) {
      Observation c = (Observation) res;
      CodeableConcept cc = c.getValueCodeableConcept();
      b = codeInSet(cc, codeSet);
      if (b) {
        //	logger.info("Patient has covid: " + pd.patient.getId());
        //	logger.info(" - " + cc.getCodingFirstRep().getCode());
        break;
      }
    }
    return b;
  }

  private boolean hasCovidCondition(PatientData pd, Set<String> codeSet) {
    boolean b = false;
    for (IBaseResource res : bundleToSet(pd.getConditions())) {
      Condition c = (Condition) res;
      CodeableConcept cc = c.getCode();
      b = codeInSet(cc, codeSet);
      if (b) {
        //	logger.info("Patient has covid: " + pd.patient.getId());
        //	logger.info(" - " + cc.getCodingFirstRep().getCode());
        pd.setPrimaryDx(cc);
        break;
      }
    }
    return b;
  }

}
