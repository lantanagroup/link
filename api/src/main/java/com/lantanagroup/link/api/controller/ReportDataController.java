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
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
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

    //for testing
    /*if(dataGovernanceConfig == null) {
      dataGovernanceConfig = new DataGovernanceConfig();
      dataGovernanceConfig.setCensusListRetention("PT4H");
      dataGovernanceConfig.setPatientDataRetention("PT4H");
      dataGovernanceConfig.setReportRetention("PT4H");
    }*/

    /*if(dataGovernanceConfig.getCensusListRetention() != null) {
      expungeData(fhirDataProvider, dataGovernanceConfig.getCensusListRetention(), String id, String type);
    }*/

    Bundle bundle = fhirDataProvider.getAllResources();
    if(bundle != null) {
      for(Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        String tag = entry.getResource().getMeta().getTag().get(0).getCode();
        Date lastUpdate = entry.getResource().getMeta().getLastUpdated();

        if(tag.equals(Constants.patientDataTag) && dataGovernanceConfig.getPatientDataRetention() != null) {
          expungeData(fhirDataProvider, dataGovernanceConfig.getPatientDataRetention(),
                  lastUpdate, entry.getResource().getId(), entry.getResource().getResourceType().toString());
        }

       if(tag.equals(Constants.reportTag) && dataGovernanceConfig.getReportRetention() != null) {
          expungeData(fhirDataProvider, dataGovernanceConfig.getReportRetention(),
                  lastUpdate, entry.getResource().getId(), entry.getResource().getResourceType().toString());
        }
      }
    }
  }

  private void expungeData(FhirDataProvider fhirDataProvider, String retentionPeriod, Date lastDatePosted, String id, String type) {
    try {
      Date comp = adjustTime(retentionPeriod, lastDatePosted);
      Date today = new Date();

      if(today.compareTo(comp) >= 0) {
        fhirDataProvider.deleteResource(type, id, true);
        logger.info(type + ": " + id + " has been expunged.");
      }
    } catch (DatatypeConfigurationException e) {
      e.printStackTrace();
    }
  }

  private Date adjustTime(String retentionPeriod, Date lastDatePosted) throws DatatypeConfigurationException {
    Duration dur = DatatypeFactory.newInstance().newDuration(retentionPeriod);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(lastDatePosted);

    calendar.add(Calendar.YEAR, dur.getYears());
    calendar.add(Calendar.MONTH, dur.getMonths());
    calendar.add(Calendar.DAY_OF_MONTH, dur.getDays());
    calendar.add(Calendar.HOUR_OF_DAY, dur.getHours());
    calendar.add(Calendar.MINUTE, dur.getMinutes());
    calendar.add(Calendar.SECOND, dur.getSeconds());

    return calendar.getTime();
  }
}
