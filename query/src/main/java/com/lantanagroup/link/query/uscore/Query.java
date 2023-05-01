package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.BaseQuery;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Query extends BaseQuery implements IQuery {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  @Autowired
  protected FhirDataProvider fhirDataProvider;

  @Override
  public void execute(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest, String reportId, List<String> resourceTypes, List<String> measureIds) {
    if (patientsOfInterest == null) {
      throw new IllegalArgumentException("patientsOfInterest");
    }

    if (measureIds == null) {
      throw new IllegalArgumentException("Measure IDs must be provided");
    }

    if (patientsOfInterest.size() > 0) {
      try {
        //EventService es = new EventService();
        PatientScoop scoop = new PatientScoop();
        scoop.setUsCoreConfig(usCoreConfig);
        scoop.setQueryConfig(queryConfig);
        // Need to set FhirDataProvider...
        //PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
        scoop.setFhirQueryServer(this.getFhirQueryClient());
        scoop.setFhirDataProvider(fhirDataProvider);
        scoop.execute(criteria, context, patientsOfInterest, reportId, resourceTypes, measureIds);
      } catch (Exception ex) {
        logger.error("Error scooping data for patients: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }
}
