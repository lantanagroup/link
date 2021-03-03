package com.lantanagroup.nandina.query.measure.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.QueryReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

public class FormQuery {
    private static final Logger logger = LoggerFactory.getLogger(com.lantanagroup.nandina.query.measure.fhir.r4.FormQuery.class);

    public FormQuery(Map<String, String> criteria, Map<String, Object> contextData, NandinaConfig properties, IGenericClient fhirStoreClient) {
        this.criteria = criteria;
        this.contextData = contextData;
        this.config = properties;
        this.fhirClient = fhirStoreClient;
    }

    private Map<String, String> criteria;
    private Map<String, Object> contextData;
    private NandinaConfig config;
    private IGenericClient fhirClient;

    public void execute() {
        FhirContext fhirContext = (FhirContext) this.contextData.get("fhirContext");
        MeasureReport measureReport = null;
        String measureId = this.contextData.get("measureId").toString();
        QueryReport queryReport = (QueryReport) this.contextData.get("report");

        String url = this.config.getFhirServerStoreBase();
        if (!url.endsWith("/")) url += "/";
        url += "Measure/" + measureId + "/$evaluate-measure?" +
                "periodStart=" + queryReport.getDate() + "&" +
                "periodEnd=" + LocalDate.parse(queryReport.getDate()).plusDays(1).toString();

        try {
            measureReport = fhirClient.fetchResourceFromUrl(MeasureReport.class, url);

            if (this.config.getMeasureLocationConfig() != null) {
                Reference subjectRef = new Reference()
                        .setIdentifier(new Identifier()
                                .setSystem(this.config.getMeasureLocationConfig().getSystem())
                                .setValue(this.config.getMeasureLocationConfig().getValue()));
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

                queryReport.setAnswer("measureReport", fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));
                //System.out.println(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));
            }
        } catch (Exception e) {
            logger.error("Error generating Measure Report - " + e.getMessage());
            queryReport.setAnswer("measureReport", e.getMessage());
        }
    }
}
