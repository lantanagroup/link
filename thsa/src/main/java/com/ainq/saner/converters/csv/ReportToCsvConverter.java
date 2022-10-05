package com.ainq.saner.converters.csv;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponentComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Quantity;

public class ReportToCsvConverter extends AbstractConverter {
    protected static final boolean SIMPLIFY_CODES = false;
    private final MeasureReport measureReport;
    private PrintWriter csvOutput;
    private final List<Integer> writeOrder = new ArrayList<>();

    public ReportToCsvConverter(Writer csvOutput, MeasureReport measureReport, Map<String, String> orderedHeaderMap) {
        super(getMeasure(measureReport), orderedHeaderMap);
        this.measureReport = measureReport;
        this.csvOutput = csvOutput instanceof PrintWriter ? (PrintWriter) csvOutput : new PrintWriter(csvOutput);
    }

    private static String getMeasureScore(Quantity measureScore) {
        if (measureScore == null || measureScore.isEmpty()) {
            return "";
        }
        if (measureScore.hasValue()) {
            String value = measureScore.getValue().toPlainString();
            if ("%".equals(measureScore.getUnit()) || "%".equals(measureScore.getCode())) {
                return value + "%";
            }
            return value;
        }
        Extension dataAbsentReason = measureScore.getExtensionByUrl(DATA_ABSENT_REASON_EXTENSION_URL);
        if (dataAbsentReason == null || !dataAbsentReason.hasValue()) {
            return "";
        }
        Coding coding = (Coding) dataAbsentReason.getValue();
        return coding.hasCode() ? coding.getCode() : "";
    }

    private void getStratumData(String[] fields, int measurePos, int pos, StratifierGroupComponent stratum) {
        if (!stratum.hasComponent()) {
            // If there are no components, output the value for this stratum.
            fields[pos] = codeToString(stratum.getValue());
        }
        for (StratifierGroupComponentComponent comp: stratum.getComponent()) {
            CodeableConcept componentCode = comp.getCode();
            pos = getFieldPosition(componentCode);
            fields[pos] = codeToString(comp.getValue());
        }
        for (StratifierGroupPopulationComponent population: stratum.getPopulation()) {
            CodeableConcept populationCode = population.getCode();
            pos = getFieldPosition(populationCode);
            fields[pos] = getPopulationCount(population.getCountElement());
        }
        fields[measurePos] = getMeasureScore(stratum.getMeasureScore());
    }

    private static String getPopulationCount(IntegerType countElement) {
        if (countElement == null || countElement.isEmpty()) {
            return "";
        }

        if (countElement.hasValue()) {
            return countElement.getValueAsString();
        }

        Extension dataAbsentReason = countElement.getExtensionByUrl(DATA_ABSENT_REASON_EXTENSION_URL);
        if (dataAbsentReason == null || !dataAbsentReason.hasValue()) {
            return "";
        }
        return dataAbsentReason.getValue().toString();
    }

    public List<List<String>> generateDataRows() {
        List<List<String>> data = new ArrayList<>();
        // Report on unstratified measure
        data.add(getMeasureData());
        // Report on strata
        getStrataData(data);
        return data;
    }

    private List<String> getMeasureData() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < getNumStrataColumns(); i++) {
            data.add("");
        }
        // For each group in the measure report
        for (MeasureReportGroupComponent group: measureReport.getGroup()) {
            //  For each population in the group
            for (MeasureReportGroupPopulationComponent population: group.getPopulation()) {
                data.add(getPopulationCount(population.getCountElement()));
            }
            // Get the Measure
            data.add(getMeasureScore(group.getMeasureScore()));
        }
        return data;
    }

    private void getStrataData(List<List<String>> data) {
        // For each distinct stratifier.code in each group
        for (MeasureReportGroupComponent group: measureReport.getGroup()) {
            int measurePos = getFieldPosition(group.getCode());
            for (MeasureReportGroupStratifierComponent stratifier: group.getStratifier()) {
                CodeableConcept stratifierCode = stratifier.getCode().get(0);
                int pos = getFieldPosition(STRATIFIER);
                //  For each distinct stratifier.component.code of each stratifier
                for (StratifierGroupComponent stratum: stratifier.getStratum()) {
                    String[] fields = new String[getNumFields()];
                    Arrays.fill(fields, "");
                    fields[pos] = codeToString(stratifierCode);
                    getStratumData(fields, measurePos, pos, stratum);
                    data.add(Arrays.asList(fields));
                }
            }
        }
    }

    public void writeRow(List<String> data) {
        boolean first = true;
        for (int col: writeOrder) {
            if (first) {
                first = false;
            } else {
                csvOutput.print(",");
            }
            String value = col >= data.size() ? "" : data.get(col);
            if (codeConverter != null) {
                value = codeConverter.apply(value);
            }
            csvOutput.print(StringEscapeUtils.escapeCsv(value));
        }
        csvOutput.println();

    }

    public void convert() {
        List<String> headers = getCanonicalHeaders();
        remapRow(headers);
        for (int i = 0; i < headers.size(); i++) {
            headers.set(i, orderedHeaderMap.get(headers.get(i)));
        }
        writeRow(headers);
        generateDataRows().forEach(this::writeRow);
        this.csvOutput.close();
    }

    private void remapRow(List<String> headers) {
        for (String header: orderedHeaderMap.keySet()) {
            int pos = indexOf(header, headers);
            if (pos >= 0) {
                writeOrder.add(pos);
            }
        }
    }

    public void setSimplifyCodes(boolean simplify) {
        if (simplify) {
            setConverter(AbstractConverter::simplifyingConverter);
        } else {
            setConverter(null);
        }
    }
}