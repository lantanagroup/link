package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.nandina.direct.Attachment;
import com.lantanagroup.nandina.direct.DirectSender;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PillboxCsvReport;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.EncounterScoop;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.ListResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PillBox {

    private static ListResource getListFromFile(FhirContext ctx, String listLocation) throws IOException {
        IParser xmlParser = ctx.newXmlParser();
        IParser jsonParser = ctx.newJsonParser();
        File encounterListFile = new File(listLocation);
        String encListStr = Files.readString(encounterListFile.toPath());
        if (encListStr.startsWith("{")) {
            return (ListResource) jsonParser.parseResource(encListStr);
        } else {
            return (ListResource) xmlParser.parseResource(encListStr);
        }
    }

    public static Date getReportDate(String date) throws ParseException {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(date);
        } else {
            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            Calendar reportCal = Calendar.getInstance();
            reportCal.setTime(now);
            reportCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
            reportCal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR));
            return reportCal.getTime();
        }
    }

    public static JsonProperties getConfig(String configLocation) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(configLocation), JsonProperties.class);
    }

    public static void main(String[] args) {
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        // Add options for CLI
        options.addOption("config", true, "Location of the config file to use with Nandina");
        options.addOption("date", true, "The date of the report in YYYY-MM-DD format. If unpopulated will use today's date");
        options.addOption("list", true, "A FHIR R4 List resource containing references to Encounter resources (by identifier). Currently optional if the nandinaFhirServer parameter is populated, as the app will query that server for the List of Encounter identifiers");
        options.addOption("save", true, "The output file location. If unpopulated, will output to standard out");
        options.addOption("send", false, "Whether or not the results should be sent via Direct specified in configuration");

        try {
            CommandLine cmd = parser.parse(options, args);
            String configLocation = cmd.getOptionValue("config");

            if (StringUtils.isEmpty(configLocation)) {
                System.err.println("You must specify the \"config\" parameter");
                return;
            }

            String date = cmd.getOptionValue("date");
            String listLocation = cmd.getOptionValue("list");
            String saveLocation = cmd.getOptionValue("save");
            FhirContext ctx = FhirContext.forR4();
            Date reportDate = getReportDate(date);
            JsonProperties config = getConfig(configLocation);

            IGenericClient targetFhirServer = null;
            IGenericClient nandinaFhirServer = null;
            EncounterScoop scoop = null;

            if (!StringUtils.isEmpty(config.getFhirServerQueryBase())) {
                targetFhirServer = ctx.newRestfulGenericClient(config.getFhirServerQueryBase());
                targetFhirServer.registerInterceptor(new HapiFhirAuthenticationInterceptor(null, config));
            }

            if (!StringUtils.isEmpty(config.getFhirServerStoreBase())) {
                nandinaFhirServer = ctx.newRestfulGenericClient(config.getFhirServerStoreBase());
            }

            if (listLocation != null) {
                ListResource encounterList = getListFromFile(ctx, listLocation);
                scoop = new EncounterScoop(targetFhirServer, nandinaFhirServer, encounterList);
            } else if (nandinaFhirServer != null) {
                scoop = new EncounterScoop(targetFhirServer, nandinaFhirServer, reportDate);
            } else {
                System.err.println("An encounter List was not specified and PillBox is not configured with a nandina-specific FHIR server. Cannot continue.");
                return;
            }

            List<Filter> filters = new ArrayList<Filter>();
            PillboxCsvReport pcr = new PillboxCsvReport(scoop, filters, ctx);

            if (cmd.hasOption("send")) {
                // this code attaches the zip file to the email
                DirectSender sender = new DirectSender(config, ctx);
                sender.send("Nandina Report", "See the attached CSV files",
                        new Attachment(pcr.getUniqueCsv().getBytes(), "text/csv", "unique.csv"),
                        new Attachment(pcr.getMedsCsv().getBytes(), "text/csv", "meds.csv"),
                        new Attachment(pcr.getDxCsv().getBytes(), "text/csv", "dx.csv"),
                        new Attachment(pcr.getLabCsv().getBytes(), "text/csv", "labs.csv"));
            }

            if (!StringUtils.isEmpty(saveLocation)) {
                File outputFile = new File(saveLocation);
                byte[] zipBytes = pcr.getReportData();
                Files.write(outputFile.toPath(), zipBytes);
                System.out.println("Output file saved to " + outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
