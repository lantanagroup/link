package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.api.config.ApiConfig;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

public class MeasureEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);

    private MeasureEvaluator(Map<String, String> criteria, Map<String, Object> contextData, ApiConfig config, IGenericClient fhirStoreClient) {
        this.criteria = criteria;
        this.contextData = contextData;
        this.config = config;
        this.fhirClient = fhirStoreClient;
    }

    public static void generateMeasureReport(Map<String, String> criteria, Map<String, Object> contextData, ApiConfig config, IGenericClient fhirStoreClient) {
        MeasureEvaluator evaluator = new MeasureEvaluator(criteria, contextData, config, fhirStoreClient);
        evaluator.generateMeasureReport();
    }

    private Map<String, String> criteria;
    private Map<String, Object> contextData;
    private ApiConfig config;
    private IGenericClient fhirClient;

    private void generateMeasureReport() {
        FhirContext fhirContext = (FhirContext) this.contextData.get("fhirContext");
        String measureId = this.contextData.get("measureId").toString();
        QueryReport queryReport = (QueryReport) this.contextData.get("report");
        MeasureReport measureReport = null;

        String url = this.config.getFhirServerStore();
        if (!url.endsWith("/")) url += "/";
        url += "Measure/" + measureId + "/$evaluate-measure?" +
                "periodStart=" + queryReport.getDate() + "&" +
                "periodEnd=" + LocalDate.parse(queryReport.getDate()).plusDays(1).toString();

        try {
            logger.info("Executing $evaluate-measure");
            measureReport = fhirClient.fetchResourceFromUrl(MeasureReport.class, url);
            logger.info("Done executing $evaluate-measure");

            if (this.config.getMeasureLocation() != null) {
                logger.debug("Creating MeasureReport.subject based on config");
                Reference subjectRef = new Reference()
                        .setIdentifier(new Identifier()
                                .setSystem(this.config.getMeasureLocation().getSystem())
                                .setValue(this.config.getMeasureLocation().getValue()));
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
                queryReport.setAnswer("measureReport", fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));
                //System.out.println(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));
            }
        } catch (Exception e) {
            logger.error("Error generating Measure Report - " + e.getMessage());
            queryReport.setAnswer("measureReport", e.getMessage());
        }
    }
}
