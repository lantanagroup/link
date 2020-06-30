package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.query.r4.cerner.PillboxCsvReport;
import com.lantanagroup.nandina.query.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.r4.cerner.scoop.EncounterScoop;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.hl7.fhir.r4.model.ListResource;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PillBox {
  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("server", true, "Base URL of the FHIR server that is the target of data extraction");
    options.addOption("in", true, "A FHIR R4 List resource containing references to Encounter resources (by identifier)");
    options.addOption("out", true, "The output file location");

    CommandLineParser parser = new DefaultParser();
		ListResource encList;

    try {
      CommandLine cmd = parser.parse(options, args);
      for (String arg : cmd.getArgList()) {
        System.out.println(arg);
      }

      String fhirServerBase = cmd.getOptionValue("fhir");
      File encounterListFile = new File(cmd.getOptionValue("in"));
      File out = new File(cmd.getOptionValue("out"));
      String encListStr = Files.readString(encounterListFile.toPath());
      FhirContext ctx = FhirContext.forR4();
      IParser xmlParser = ctx.newXmlParser();
      IParser jsonParser = ctx.newJsonParser();

      if (encListStr.startsWith("{")) {
        encList = (ListResource) jsonParser.parseResource(encListStr);
      } else {
        encList = (ListResource) xmlParser.parseResource(encListStr);
      }

      // same FHIR server for nandina and target for now
      IGenericClient targetFhirServer = ctx.newRestfulGenericClient(fhirServerBase);
      IGenericClient nandinaFhirServer = ctx.newRestfulGenericClient(fhirServerBase);
      EncounterScoop scoop = new EncounterScoop(targetFhirServer, nandinaFhirServer, encList);

      //	Scoop scoop = new Scoop(targetFhirServer,nandinaFhirServer , sdf.parse("2020-05-04"));
      List<Filter> filters = new ArrayList<Filter>();
      PillboxCsvReport pcr = new PillboxCsvReport(scoop, filters);
      byte[] zipBytes = pcr.getReportData();
      Files.write(out.toPath(), zipBytes);
      System.out.println("Output file saved to " + out.getAbsolutePath());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
