package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.BaseQuery;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Query extends BaseQuery implements IQuery {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  @Override
  public void execute(List<PatientOfInterestModel> patientsOfInterest, String reportId, List<String> resourceTypes, String measureId) {
    if (patientsOfInterest == null) {
      throw new IllegalArgumentException("patientsOfInterest");
    }

    if (StringUtils.isEmpty(measureId)) {
      throw new IllegalArgumentException("Measure Id must be provided");
    }

    if (patientsOfInterest.size() > 0) {
      try {
        PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
        scoop.setFhirQueryServer(this.getFhirQueryClient());
        scoop.execute(patientsOfInterest, reportId, resourceTypes, measureId);
      } catch (Exception ex) {
        logger.error("Error scooping data for patients: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }
}
