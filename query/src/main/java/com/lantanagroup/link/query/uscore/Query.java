package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.BaseQuery;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Query extends BaseQuery implements IQuery {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  @Override
  public void execute(ReportCriteria criteria, ReportContext context, QueryPhase queryPhase) {
    List<PatientOfInterestModel> patientsOfInterest = context.getPatientsOfInterest(queryPhase);
    if (patientsOfInterest.size() > 0) {
      try {
        PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
        scoop.setFhirQueryServer(this.getFhirQueryClient());
        scoop.execute(criteria, context, patientsOfInterest, queryPhase);
      } catch (Exception ex) {
        logger.error("Error scooping data for patients", ex);
      }
    }
  }
}
