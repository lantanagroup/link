package com.lantanagroup.nandina.query.measure.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.query.BaseFormQuery;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class FormQuery extends BaseFormQuery {
    private static final Logger logger = LoggerFactory.getLogger(com.lantanagroup.nandina.query.measure.fhir.r4.FormQuery.class);

    @Override
    public void execute() {
        FhirContext fhirContext = (FhirContext) this.getContextData("fhirContext");
        MeasureReport measureReport = null;
        String measureId = this.getContextData("measureId").toString();
        QueryReport queryReport = (QueryReport) this.getContextData("report");

        String url = this.getProperties().getFhirServerStoreBase();
        if (!url.endsWith("/")) url += "/";
        url += "Measure/" + measureId + "/$evaluate-measure?" +
                "periodStart=" + queryReport.getDate() + "&" +
                "periodEnd=" + LocalDate.parse(queryReport.getDate()).plusDays(1).toString();
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(this.getProperties().getFhirServerStoreBase());
        fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 5000);

        try {
            measureReport = fhirClient.fetchResourceFromUrl(MeasureReport.class, url);

            if (this.getProperties().getMeasureLocationConfig() != null) {
                Reference subjectRef = new Reference()
                        .setIdentifier(new Identifier()
                                .setSystem(this.getProperties().getMeasureLocationConfig().getSystem())
                                .setValue(this.getProperties().getMeasureLocationConfig().getValue()));
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

                this.setAnswer("measureReport", fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));
                System.out.println(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport));
            }
        } catch (Exception e) {
            logger.error("Error generating Measure Report - " + e.getMessage());
            this.setAnswer("measureReport", e.getMessage());
        }
    }
}
