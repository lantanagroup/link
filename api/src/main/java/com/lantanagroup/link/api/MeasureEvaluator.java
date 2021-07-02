package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.config.api.ApiConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

public class MeasureEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);
    public static final String POSITION_EXT_URL = "http://hl7.org/fhir/uv/saner/StructureDefinition/GeoLocation";

    private MeasureEvaluator(Map<String, String> criteria,  Map<String, Object> contextData, ApiConfig config, IGenericClient fhirStoreClient) {
        this.criteria = criteria;
        this.contextData = contextData;
        this.config = config;
        this.fhirClient = fhirStoreClient;
    }

    public static MeasureReport generateMeasureReport(Map<String, String> criteria, Map<String, Object> contextData, ApiConfig config, IGenericClient fhirStoreClient) {
        MeasureEvaluator evaluator = new MeasureEvaluator(criteria, contextData, config, fhirStoreClient);
        return evaluator.generateMeasureReport();
    }

    private Map<String, String> criteria;
    private Map<String, Object> contextData;
    private ApiConfig config;
    private IGenericClient fhirClient;

    private MeasureReport generateMeasureReport() {
        FhirContext fhirContext = (FhirContext) this.contextData.get("fhirContext");
        String measureId = this.contextData.get("measureId").toString();
        String reportId = this.contextData.get("reportId").toString();
        QueryReport queryReport = (QueryReport) this.contextData.get("report");
        MeasureReport measureReport = null;

        String url = this.config.getFhirServerStore();
        if (!url.endsWith("/")) url += "/";
        url += "Measure/" + measureId + "/$evaluate-measure?" +
                "periodStart=" + queryReport.getDate() + "&" +
                "periodEnd=" + LocalDate.parse(queryReport.getDate()).plusDays(1).toString();

        try {
            logger.info(String.format("Executing $evaluate-measure for %s", measureId));
            measureReport = fhirClient.fetchResourceFromUrl(MeasureReport.class, url);
            logger.info(String.format("Done executing $evaluate-measure for %s", measureId));

            if (this.config.getMeasureLocation() != null) {
                logger.debug("Creating MeasureReport.subject based on config");
                Reference subjectRef = new Reference();

                if (this.config.getMeasureLocation().getSystem() != null || this.config.getMeasureLocation().getValue() != null) {
                    subjectRef.setIdentifier(new Identifier()
                        .setSystem(this.config.getMeasureLocation().getSystem())
                        .setValue(this.config.getMeasureLocation().getValue()));
                }

                if (this.config.getMeasureLocation().getLatitude() != null || this.config.getMeasureLocation().getLongitude() != null) {
                    Extension positionExt = new Extension(POSITION_EXT_URL);

                    if (this.config.getMeasureLocation().getLongitude() != null) {
                        Extension longExt = new Extension("longitude");
                        longExt.setValue(new DecimalType(this.config.getMeasureLocation().getLongitude()));
                        positionExt.addExtension(longExt);
                    }

                    if (this.config.getMeasureLocation().getLatitude() != null) {
                        Extension latExt = new Extension("latitude");
                        latExt.setValue(new DecimalType(this.config.getMeasureLocation().getLatitude()));
                        positionExt.addExtension(latExt);
                    }

                    subjectRef.addExtension(positionExt);
                }

                measureReport.setSubject(subjectRef);
            }

            // TODO: commenting out this code because the narrative text isn't being generated, will need to look into this
            // fhirContext.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
            // String output = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport);

            if (null != measureReport) {
                // Fix the measure report's evaluatedResources to make sure resource references are correctly formatted
                for (Reference evaluatedResource : measureReport.getEvaluatedResource()) {
                    if (!evaluatedResource.hasReference()) continue;

                    if (evaluatedResource.getReference().matches("^#[A-Z].+/.+$")) {
                        String newReference = evaluatedResource.getReference().substring(1);
                        evaluatedResource.setReference(newReference);
                    }
                }

                logger.info("Done generating measure report, setting response answer to JSON of MeasureReport");
                measureReport.setId(reportId);
                queryReport.setAnswer("measureReport", fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));

            }
        } catch (Exception e) {
            logger.error("Error generating Measure Report - " + e.getMessage());
            queryReport.setAnswer("measureReport", e.getMessage());
        }
        return measureReport;
    }
}
