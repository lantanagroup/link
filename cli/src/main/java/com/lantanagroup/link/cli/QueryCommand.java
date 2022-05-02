package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import com.lantanagroup.link.query.auth.*;
import com.lantanagroup.link.query.uscore.Query;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.apache.http.client.HttpResponseException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
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
  public void query(String patient, String resourceTypes, @ShellOption(defaultValue = "") String output) {
    try {
      this.registerBeans();

      QueryConfig config = this.applicationContext.getBean(QueryConfig.class);
      if(config.isRequireHttps() && !config.getFhirServerBase().contains("https")) {
        logger.error("Error, Query URL requires https");
        throw new HttpResponseException(500, "Internal Server Error");
      }

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

      List<String> resourceTypesList = new ArrayList<>();
      for (String resourceType : resourceTypes.split(",")) {
        resourceTypesList.add(resourceType);
      }

      logger.info("Executing query");

      List<QueryResponse> queryResponses = query.execute(patientsOfInterest, resourceTypesList);

      if (queryResponses != null) {
        for (int i = 0; i < queryResponses.size(); i++) {
          QueryResponse queryResponse = queryResponses.get(i);
          String patientDataXml = FhirContext.forR4().newXmlParser().encodeResourceToString(queryResponse.getBundle());

          if (Strings.isNotEmpty(output)) {
            String file = (!output.endsWith("/") ? output + FileSystems.getDefault().getSeparator() : output) + "patient-" + (i + 1) + ".xml";
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(patientDataXml.getBytes(StandardCharsets.UTF_8));
            fos.close();
            logger.info("Stored patient data XML to " + file);
          } else {
            System.out.println("Patient " + (i + 1) + " Bundle XML:");
            System.out.println(patientDataXml);
          }
        }
      }

      logger.info("Done");
    } catch (Exception ex) {
      logger.error("Error executing query: " + ex.getMessage(), ex);
    }
  }
}
