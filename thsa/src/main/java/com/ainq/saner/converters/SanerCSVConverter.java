package com.ainq.saner.converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import com.ainq.fhir.utils.YamlUtils;
import com.ainq.saner.converters.csv.CSVConversionException;
import com.ainq.saner.converters.csv.CsvToReportConverter;
import com.ainq.saner.converters.csv.ReportToCsvConverter;
import com.ainq.saner.converters.csv.Util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

/**
 * This class performs Conversion between a MeasureReport and CSV Format
 * as defined by the HL7 SANER Implementation Guide.
 *
 * @author Keith W. Boone
 *
 */
public class SanerCSVConverter {

    private static FhirContext ctx = FhirContext.forR4();
    protected static IParser jp = ctx.newJsonParser().setPrettyPrint(true);
    protected static IParser xp = ctx.newXmlParser().setPrettyPrint(true);
    protected static IParser yp = YamlUtils.newYamlParser(ctx).setPrettyPrint(true);
    private static int errors = 0;

    /**
     * Hide the default public constructor.
     */
    protected SanerCSVConverter() {
        // Hiding the constructor
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[] { "src/test/resources/MeasureReport-CSVExportExample.json" };
        }
        File csvFile = null;
        File outputFile = null;
        Map<String, String> columns = new TreeMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (csvFile != null) {
                MeasureReport mr = convertCsvToResource(csvFile, new File(arg), null, Util.invertMap(columns), null);
                writeMeasureReport(new FileWriter(outputFile), mr);
                csvFile = null;
                columns.clear();
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                usage();
            } else if (arg.contains("=")) {
                String[] parts = arg.split("=");
                columns.put(parts[0], parts.length > 1 ? parts[1] : "");
            } else if (arg.endsWith(".xml") || arg.endsWith(".json")) {
                File f = new File(arg);
                String basename = StringUtils.substringBeforeLast(f.getName(),".");
                outputFile = new File("target", basename + ".csv");
                System.out.println("Output: " + outputFile.getAbsolutePath());
                MeasureReport mr = getResource(MeasureReport.class, f);
                convertMeasureReportToCSV(mr, columns, new FileWriter(outputFile), true);
                columns.clear();
            } else if (arg.endsWith(".csv")) {
                csvFile = new File(arg);
                String basename = StringUtils.substringBeforeLast(csvFile.getName(),".");
                outputFile = new File("target", basename + ".json");
            }
        }
        if (csvFile != null) {
            System.err.println("No measure provided for CSV conversion");
            errors++;
        }

        System.out.printf("%d errors%n", errors);
        System.exit(errors);
    }

    private static void usage() {
        System.out.printf("Usage:%n"
            + "\tjava %s [code=columnName]* (measureReport.{xml|json} | measureReport.csv measure.{xml|json})%n%n"
            + "Where:%n"
            + "\tcode=columnName Maps the measureReport count or measureScore%n"
            + "\t                identified by code to the specified column%n%n"
            + "\tmeasureReport.{xml|json} is a MeasureReport resource to convert to CSV%n"
            + "\tmeasureReport.csv is a converted CSV File%n"
            + "\tmeasure.{xml|json} is a Measure resource that defines how a MeasureReport will be generated",
                SanerCSVConverter.class.getName() );
    }

    protected static <R extends Resource> R getResource(Class<R> cz, File f) throws IOException {
        if (!f.exists()) {
            errors++;
            throw new IOException("File " + f + " does not exist.");
        }
        try (FileReader r = new FileReader(f)) {
            if (f.getName().endsWith(".xml")) {
                return xp.parseResource(cz, r);
            } else if (f.getName().endsWith(".yaml")) {
                return yp.parseResource(cz, r);
            } else {
            	System.out.println("Processing JSON MeasureReport");
                return jp.parseResource(cz, r);
            }
        } catch (FileNotFoundException e) {
            System.err.printf("File %s could not be found.%n", f);
        } catch (IOException e) {
            System.err.printf("Error reading %s.%n", f);
        } catch (DataFormatException e) {
            System.err.printf("Error parsing %s.%n", f);
        }
        errors++;
        return null;
    }

    public static MeasureReport convertCsvToResource(
        File f, File measureFile, Reference subject, Map<String, String> columns, UnaryOperator<String> converter) throws IOException {
        Measure measure = getResource(Measure.class, measureFile);
        MeasureReport mr = null;
        try (FileReader r = new FileReader(f, StandardCharsets.UTF_8)) {
            mr = convertCSVToMeasureReport(r, measure, subject, columns, converter);
            mr.setId(StringUtils.substringBefore(f.getName(), "."));
        }
        return mr;
    }

    protected static void writeMeasureReport(Writer writer, MeasureReport mr) throws IOException {
        try {
            yp.encodeResourceToWriter(mr, writer);
        } catch (DataFormatException | IOException e) {
            errors++;
            throw e;
        }
    }

    /**
     * Convert a measureReport from FHIR Resource format to CSV format.
     * @param measureReport The FHIR MeasureReport to convert.
     * @param orderedHeaderMap The map from canonical headers to output header names.  Fields are reported in the order that map.entries() returns keys. See {@link java.util.LinkedHashMap}.
     * @param csvOutput The place to store the CSV Output
     * @param simplify If true, simplify codes in strata on output, if false, report a system#code
     * @throws CSVConversionException   On errors converting to CSV Format
     */
    public static void convertMeasureReportToCSV(MeasureReport measureReport, Map<String, String> orderedHeaderMap, Writer csvOutput, boolean simplify) throws IOException {
        ReportToCsvConverter converter = new ReportToCsvConverter(csvOutput, measureReport, orderedHeaderMap);
        converter.setSimplifyCodes(simplify);
        converter.convert();
        // Added close() since was not flushing to file when a FileWriter was passed.
        csvOutput.close();
    }

    /**
     * Convert a CSV to a MeasureReport
     * @param r A reader that accesses the CSV Content
     * @param measure   The measure embodied in the CSV File
     * @param subject   The subject to associate with the MeasureReport
     * @param orderedHeaderMap  A map describing how columns are produced from groups, population and strata in the measure
     * @param codeConverter A function which converts simplified strings (see simplify in convertMeasureReportToCSV) to proper values
     * @return  The CSV as a MeasureReport resource
     * @throws IOException  On errors reading the CSV file
     */
    public static MeasureReport convertCSVToMeasureReport(
        Reader r, Measure measure, Reference subject, Map<String, String> orderedHeaderMap, UnaryOperator<String> codeConverter) throws IOException {
        CsvToReportConverter converter = new CsvToReportConverter(measure, subject, orderedHeaderMap);
        if (codeConverter != null) {
            converter.setConverter(codeConverter);
        }
        return converter.convert(r);
    }

}
