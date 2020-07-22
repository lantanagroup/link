package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.query.fhir.r4.cerner.PillboxCsvReport;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hl7.fhir.r4.model.ListResource;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PillBox {
	
  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("targetFhirServer", true, "Base URL of the FHIR server that is the target of data extraction");
    Option tfs = options.getOption("targetFhirServer");
    tfs.setRequired(true);
    options.addOption("date", true, "The date of the report in YYYY-MM-DD format. If unpopulated will use today's date");
    options.addOption("nandinaFhirServer", true, "Base URL of the FHIR server that stores Nandina Pillbox data. Currently optional if the 'in' parameter is populated");
    options.addOption("in", true, "A FHIR R4 List resource containing references to Encounter resources (by identifier). Currently optional if the nandinaFhirServer parameter is populated, as the app will query that server for the List of Encounter identifiers");
    options.addOption("out", true, "The output file location. If unpopulated, will output to standard out");

    CommandLineParser parser = new DefaultParser();
	ListResource encList = null;

    try {
      CommandLine cmd = parser.parse(options, args);
      for (String arg : cmd.getArgList()) {
        System.out.println(arg);
      }

      String targetFhirServerString = cmd.getOptionValue("targetFhirServer");
      if (targetFhirServerString == null) {
    	  throw new Exception ("Argument targetFhirServer is required.");
      }
      String date = cmd.getOptionValue("date");
      String nandinaFhirServerString = cmd.getOptionValue("nandinaFhirServer");
      String in = cmd.getOptionValue("in");
      String out = cmd.getOptionValue("out");
      FhirContext ctx = FhirContext.forR4();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date reportDate;
      if (date != null) {
    	  reportDate = sdf.parse(date);
      } else {
    	  Date now = new Date();
    	  Calendar cal = Calendar.getInstance();
    	  Calendar reportCal = Calendar.getInstance();
    	  reportCal.setTime(now);
    	  reportCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
    	  reportCal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR));
    	  reportDate = reportCal.getTime();
      }
      IGenericClient targetFhirServer = ctx.newRestfulGenericClient(targetFhirServerString);
      IGenericClient nandinaFhirServer = null;
      EncounterScoop scoop = null;
      if (in != null ) {
          IParser xmlParser = ctx.newXmlParser();
          IParser jsonParser = ctx.newJsonParser();
          File encounterListFile = new File(in);
          String encListStr = Files.readString(encounterListFile.toPath());
          if (encListStr.startsWith("{")) {
            encList = (ListResource) jsonParser.parseResource(encListStr);
          } else {
            encList = (ListResource) xmlParser.parseResource(encListStr);
          }
          scoop = new EncounterScoop(targetFhirServer, nandinaFhirServer, encList);
      } else if (nandinaFhirServerString != null) {
          nandinaFhirServer = ctx.newRestfulGenericClient(nandinaFhirServerString);
          scoop = new EncounterScoop(targetFhirServer,nandinaFhirServer , reportDate);
      }


      //	Scoop scoop = new Scoop(targetFhirServer,nandinaFhirServer , sdf.parse("2020-05-04"));
      List<Filter> filters = new ArrayList<Filter>();
      PillboxCsvReport pcr = new PillboxCsvReport(scoop, filters);
      byte[] zipBytes = pcr.getReportData();

      File outputFile = new File(cmd.getOptionValue("out"));
      Files.write(outputFile.toPath(), zipBytes);
      System.out.println("Output file saved to " + outputFile.getAbsolutePath());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
