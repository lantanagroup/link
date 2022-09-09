package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.config.datagovernance.DataGovernanceConfig;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Date;

@RestController
@RequestMapping("/api")
public class ReportDataController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @Autowired
  @Setter
  private ApplicationContext context;

  @Autowired
  private DataGovernanceConfig dataGovernanceConfig;

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  @PostMapping(value = "/data/{type}")
  public void retrieveData(@RequestBody() byte[] content, @PathVariable("type") String type) throws Exception {
    if (config.getDataProcessor() == null || config.getDataProcessor().get(type) == null || config.getDataProcessor().get(type).equals("")) {
      throw new IllegalStateException("Cannot find data processor.");
    }

    logger.debug("Receiving " + type + " data. Parsing...");

    Class<?> dataProcessorClass = Class.forName(this.config.getDataProcessor().get(type));
    IDataProcessor dataProcessor = (IDataProcessor) this.context.getBean(dataProcessorClass);

    dataProcessor.process(content, getFhirDataProvider());
  }

  @DeleteMapping(value = "/data/expunge")
  public void expungeData() {
    FhirDataProvider fhirDataProvider = getFhirDataProvider();
    /*if(tag.equals(Constants.patientDataTag) && dataGovernanceConfig.getCensusListRetention() != null) {

    }*/

    Bundle bundle = fhirDataProvider.getAllResources();
    if(bundle != null) {
      for(Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        String tag = entry.getResource().getMeta().getTag().get(0).getCode();
        Date lastUpdate = entry.getResource().getMeta().getLastUpdated();
        /*if(tag.equals(Constants.patientDataTag) && dataGovernanceConfig.getPatientDataRetention() != null) {
          String retention = dataGovernanceConfig.getPatientDataRetention();
        }*/

       /*if(tag.equals(Constants.reportTag) && dataGovernanceConfig.getReportRetention() != null) {

        }*/
      }
    }
  }

  private void expungeData(FhirDataProvider fhirDataProvider, String retentionPeriod, String id, String type) {
    fhirDataProvider.deleteResource(type, id, true);
    logger.info(type + ":" + id + " has been expunged.");
  }
}
