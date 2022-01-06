package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import com.lantanagroup.link.query.auth.*;
import com.lantanagroup.link.query.uscore.Query;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ShellComponent
public class QueryCommand extends BaseShellCommand {
  private static final Logger logger = LoggerFactory.getLogger(QueryCommand.class);

  @Override
  protected List<Class> getBeanClasses() {
    return List.of(QueryConfig.class, Query.class, PatientScoop.class, USCoreConfig.class, EpicAuth.class, EpicAuthConfig.class, CernerAuth.class, CernerAuthConfig.class, BasicAuth.class, BasicAuthConfig.class);
  }

  @ShellMethod(value = "Query for patient data from the configured FHIR server")
  public void query(String patient, @ShellOption(defaultValue = "") String output) {
    try {
      this.registerBeans();

      QueryConfig config = this.applicationContext.getBean(QueryConfig.class);

      List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
      IQuery query = QueryFactory.getQueryInstance(this.applicationContext, config.getQueryClass());

      for (String pid : patient.split(",")) {
        PatientOfInterestModel poi = new PatientOfInterestModel();
        if (pid.indexOf("/") > 0) {
          poi.setReference(pid);
        } else if (pid.indexOf("|") > 0) {
          poi.setIdentifier(pid);
        }
        patientsOfInterest.add(poi);
      }

      logger.info("Executing query");

      Bundle patientDataBundle = query.execute(patientsOfInterest);

      if (patientDataBundle.hasEntry()) {
        String patientDataXml = FhirContext.forR4().newXmlParser().encodeResourceToString(patientDataBundle);

        if (Strings.isNotEmpty(output)) {
          FileOutputStream fos = new FileOutputStream(output);
          fos.write(patientDataXml.getBytes(StandardCharsets.UTF_8));
          fos.close();
          logger.info("Stored patient data XML to " + output);
        } else {
          System.out.println(patientDataXml);
        }
      }

      logger.info("Done");
    } catch (Exception ex) {
      logger.error("Error executing query: " + ex.getMessage(), ex);
    }
  }
}
