package com.lantanagroup.link;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.PatientList;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class PatientIdService {

  private static final Logger logger = LoggerFactory.getLogger(PatientIdService.class);

  @Autowired
  @Setter
  protected ApplicationContext context;

  public void storePatientList(TenantService tenantService, PatientList patientList) throws Exception {
    logger.info("Storing patient list");
    PatientList found = tenantService.findPatientList(patientList.getPeriodStart(), patientList.getPeriodEnd(), patientList.getMeasureId());

    // Merge the list of patients found with the new list
    if (found != null) {
      logger.info("Merging with pre-existing patient list with {} entries that has {} (measure) {} (start) and {} (end)",
              found.getPatients().size(),
              found.getMeasureId(),
              found.getPeriodStart(),
              found.getPeriodEnd());
      patientList.setId(found.getId());
      found.merge(patientList);
      logger.info("Merged list contains {} entries", found.getPatients().size());
    } else {
      logger.info("No pre-existing patient list found");
      found = patientList;
    }

    tenantService.savePatientList(found);
  }
}
