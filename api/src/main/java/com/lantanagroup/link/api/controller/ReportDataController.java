package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.config.datagovernance.DataGovernanceConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.PatientData;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReportDataController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @Autowired
  @Setter
  @Getter
  private DataGovernanceConfig dataGovernanceConfig;

  @Autowired
  private MongoService mongoService;

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  private static boolean shouldDelete(String retentionPeriod, Date lastDatePosted) throws DatatypeConfigurationException {
    Date comp = adjustTime(retentionPeriod, lastDatePosted);
    Date today = new Date();
    return today.compareTo(comp) >= 0;
  }

  /**
   * @param retentionPeriod how long to keep the data
   * @param lastDatePosted  when the data was last updated
   * @return the adjusted day from when the data had been last updated to the end of its specified retention period
   * @throws DatatypeConfigurationException
   */
  private static Date adjustTime(String retentionPeriod, Date lastDatePosted) throws DatatypeConfigurationException {
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

  /**
   * @throws DatatypeConfigurationException
   * Deletes all census lists, patient data bundles, and measure reports stored on the server if their retention period
   * has been reached.
   */
  @DeleteMapping(value = "/data/expunge")
  public void expungeData() throws DatatypeConfigurationException {
    if (this.dataGovernanceConfig == null) {
      logger.error("Data governance not configured");
      return;
    }

    List<PatientData> allPatientData = this.mongoService.getAllPatientData();
    List<String> patientDataToDelete = allPatientData.stream().filter(pd -> {
              try {
                return shouldDelete(this.dataGovernanceConfig.getPatientDataRetention(), pd.getRetrieved());
              } catch (DatatypeConfigurationException e) {
                return false;
              }
            })
            .map(pd -> pd.getId())
            .collect(Collectors.toList());
    this.mongoService.deletePatientData(patientDataToDelete);
  }
}
