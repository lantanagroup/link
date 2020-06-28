package com.lantanagroup.nandina;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.hl7.fhir.r4.model.ListResource;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.EncounterDateFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.Filter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.HospitalizedReport;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.PillboxCsvReport;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.Scoop;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class PillBox {
	

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("fhir", true, "Base URL of the FHIR server that is the target of data extraction");
		options.addOption("in", true, "A FHIR R4 List resource containing references to Encounter resources (by identifier)");
		options.addOption("out", true, "The output file location");
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse( options, args);
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
			ListResource encList;
			if (encListStr.startsWith("{")) {
				encList = (ListResource)jsonParser.parseResource(encListStr);
			} else {
				encList = (ListResource)xmlParser.parseResource(encListStr);
			}
			Scoop scoop = new Scoop(fhirServerBase, encList);
			List<Filter> filters = new ArrayList<Filter>();
			PillboxCsvReport pcr = new PillboxCsvReport(fhirServerBase, scoop, filters);
			byte[] zipBytes = pcr.getReportData();
			Files.write(out.toPath(), zipBytes);
			System.out.println("Output file saved to " + out.getAbsolutePath());
			
			// The following 4 lines are an example of getting Nandina style counts
			EncounterDateFilter edf = new EncounterDateFilter("2020-05-04");
			filters.add(edf);
			HospitalizedReport hr = new HospitalizedReport(fhirServerBase, scoop, filters);
			System.out.println("Patients hospitalized with Covid on " + edf.getDateAsString() + ": " + hr.getReportCount());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
