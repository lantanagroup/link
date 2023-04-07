package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.TenantService;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.IQuery;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Query implements IQuery {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  @Setter
  private ApplicationContext applicationContext;

  @Override
  public void execute(TenantService tenantService, ReportCriteria criteria, ReportContext context, List<String> resourceTypes, List<String> measureIds) {
    List<PatientOfInterestModel> patientsOfInterest = context.getPatientsOfInterest();

    if (patientsOfInterest == null) {
      throw new IllegalArgumentException("patientsOfInterest");
    }

    if (measureIds == null) {
      throw new IllegalArgumentException("Measure IDs must be provided");
    }

    if (patientsOfInterest.size() > 0) {
      try {
        PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
        scoop.setTenantService(tenantService);
        scoop.execute(criteria, context, patientsOfInterest, resourceTypes, measureIds);
      } catch (Exception ex) {
        logger.error("Error scooping data for patients: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }
}
