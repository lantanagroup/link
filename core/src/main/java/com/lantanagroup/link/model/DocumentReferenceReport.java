package com.lantanagroup.link.model;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.*;
import java.util.Date;

@Getter
@Setter
public class DocumentReferenceReport {
    String identifier;
    String version;
    String status;
    Date date;
    Measure measure;
    MeasureReport measureReport;
    String questionnaire;
    String questionnaireReport;

    public DocumentReferenceReport(IGenericClient client, String id) throws Exception{

        Bundle documentReferences = client.search()
                .forResource("DocumentReference")
                .where(DocumentReference.IDENTIFIER.exactly().identifier(id))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        if (documentReferences.hasEntry() && !documentReferences.getEntry().get(0).isEmpty()) {
            DocumentReference documentReference = (DocumentReference) documentReferences.getEntry().get(0).getResource();

            Bundle measureBundle = client.search()
                    .forResource("Measure")
                    .where(Measure.IDENTIFIER.exactly().identifier(documentReference.getIdentifier().get(0).getValue()))
                    .returnBundle(Bundle.class)
                    .cacheControl(new CacheControlDirective().setNoCache(true))
                    .execute();

            this.measureReport = client.read()
                    .resource(MeasureReport.class)
                    .withId(documentReference.getMasterIdentifier().getValue())
                    .cacheControl(new CacheControlDirective().setNoCache(true))
                    .execute();
            this.measureReport = !this.measureReport.isEmpty() ? this.measureReport : null;

            //Assuming that each measure has a unique identifier (only one measure returned per id)
            this.measure = measureBundle.hasEntry() && !measureBundle.getEntry().get(0).isEmpty() ? (Measure) measureBundle.getEntry().get(0).getResource() : null;

            if(this.measure != null) {
                //Trying to fix an issue where meta was infinitely large due to a recursively occurring element
                String metaVersion = this.measure.getMeta().getVersionId();
                Date metaLastUpdate = this.measure.getMeta().getLastUpdated();
                String metaSource = this.measure.getMeta().getSource();

                this.measure.setMeta(null);

                Meta measureMeta = new Meta();

                measureMeta.setVersionId(metaVersion);
                measureMeta.setLastUpdated(metaLastUpdate);
                measureMeta.setSource(metaSource);

                this.measure.setMeta(measureMeta);
            }

            this.identifier = id;
            this.version = "";
            this.status = documentReference.getStatus().toString();
            this.date = documentReference.getDate();

            //empty for now
            this.questionnaire = "";
            this.questionnaireReport = "";

        }
        else {
            throw new HttpResponseException(404, "Measure Report ID does not exist");
        }

    }

}
