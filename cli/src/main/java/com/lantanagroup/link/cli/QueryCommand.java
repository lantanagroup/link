package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import org.apache.commons.cli.*;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@SpringBootApplication(scanBasePackages = {
        "com.lantanagroup.link.cli",
        "com.lantanagroup.link.config.query",
        "com.lantanagroup.link.query"
})
public class QueryCommand implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(QueryCommand.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private QueryConfig config;

  public static void main(String[] args) {
    try {
      CommandLine cmd = parseArgs(args);
      String config = cmd.getOptionValue("config");

      Properties properties = System.getProperties();
      properties.setProperty("spring.config.location", config);
    } catch (Exception ex) {
      logger.error("Error parsing args and/or setting config location: " + ex.getMessage(), ex);
    }

    new SpringApplicationBuilder(QueryCommand.class).web(WebApplicationType.NONE).run(args);
  }

  public static CommandLine parseArgs(String... args) throws ParseException {
    Options options = new Options();
    CommandLineParser parser = new DefaultParser();

    Option patientIdOpt = new Option("pid", true, "Patient Identifier (i.e. \"Patient/XXXX\" or \"<system>|<value>\")");
    patientIdOpt.setRequired(true);
    patientIdOpt.setArgs(Option.UNLIMITED_VALUES);
    options.addOption(patientIdOpt);

    Option configOpt = new Option("config", true, "The config file location");
    configOpt.setRequired(true);
    options.addOption(configOpt);

    Option outputOpt = new Option("output", true, "The output file/location");
    options.addOption(outputOpt);

    return parser.parse(options, args, true);
  }

  @Override
  public void run(String... args) throws Exception {
    try {
      CommandLine cmd = parseArgs(args);
      String[] pids = cmd.getOptionValues("pid");
      List<PatientOfInterestModel> pois = new ArrayList<>();

      IQuery query = QueryFactory.getQueryInstance(this.applicationContext, this.config.getQueryClass());

      for (String pid : pids) {
        PatientOfInterestModel poi = new PatientOfInterestModel();
        if (pid.indexOf("/") > 0) {
          poi.setReference(pid);
        } else if (pid.indexOf("|") > 0) {
          poi.setIdentifier(pid);
        }
        pois.add(poi);
      }

      logger.info("Executing query");

      Bundle patientDataBundle = query.execute(pois);

      String patientDataXml = FhirContext.forR4().newXmlParser().encodeResourceToString(patientDataBundle);

      if (cmd.hasOption("output")) {
        FileOutputStream fos = new FileOutputStream(cmd.getOptionValue("output"));
        fos.write(patientDataXml.getBytes(StandardCharsets.UTF_8));
        fos.close();
        logger.info("Stored patient data XML to " + cmd.getOptionValue("output"));
      } else {
        System.out.println(patientDataXml);
      }

      logger.info("Done");
    } catch (Exception ex) {
      logger.error("Error executing query: " + ex.getMessage(), ex);
    }
  }
}
