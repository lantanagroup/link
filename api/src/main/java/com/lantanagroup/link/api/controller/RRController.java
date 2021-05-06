package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.fhir.transform.FHIRTransformResult;
import com.lantanagroup.fhir.transform.FHIRTransformer;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;

/**
 * Reportability Response Controller
 */
@RestController
public class RRController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(RRController.class);
    private FhirContext ctx = FhirContext.forR4();

    private void receiveFHIR(Resource resource, HttpServletRequest request) throws Exception {
        IGenericClient fhirStoreClient = this.getFhirStoreClient(null, request);
        MethodOutcome outcome = null;
        if (resource.hasId()) {
            resource.setId((String) null);
        }
        if (resource instanceof ListResource) {

            List<Identifier> identifierList = ((ListResource) resource).getIdentifier();
            Date datetime = ((ListResource) resource).getDate();
            // added some validation code
            if (datetime == null) {
                throw new Exception("Date is not passed in the file.");
            }
            if(identifierList.isEmpty()){
                throw new Exception("Identifier is not present.");
            }
            String system = ((ListResource) resource).getIdentifier().get(0).getSystem();
            String value = ((ListResource) resource).getIdentifier().get(0).getValue();
            String date = getDateWithoutTimeUsingFormat(datetime);
            Bundle bundle = searchListByCodeData(system, value, date, fhirStoreClient);
            if (bundle.getEntry().size() == 0) {
               ((ListResource) resource).setDateElement(new DateTimeType(date));
                createResource(resource, fhirStoreClient);
            } else {
                ListResource existingList = (ListResource) bundle.getEntry().get(0).getResource();
                // filter out duplicates
                List<ListResource.ListEntryComponent> uniqueEntries = ((ListResource) resource).getEntry().parallelStream()
                        .filter(e -> !existingList.getEntry().stream().anyMatch(s -> e.getItem().getIdentifier().getSystem().equals(s.getItem().getIdentifier().getSystem())
                                && e.getItem().getIdentifier().getValue().equals(s.getItem().getIdentifier().getValue()))).collect(Collectors.toList());
                // merge lists into existingList
                uniqueEntries.parallelStream().forEach(entry -> {
                    existingList.getEntry().add(entry);
                });
                ((ListResource) resource).setDateElement(new DateTimeType(date));
                updateResource(existingList, fhirStoreClient);
            }
        } else {
            createResource(resource, fhirStoreClient);
        }
    }

    private void createResource(Resource resource, IGenericClient fhirStoreClient) {
        MethodOutcome outcome = fhirStoreClient.create().resource(resource).execute();
        if (!outcome.getCreated() && outcome.getResource() != null) {
            logger.error("Failed to store/create FHIR Bundle");
        } else {
            logger.debug("Stored FHIR Bundle with new ID of " + outcome.getResource().getIdElement().getIdPart());
        }
    }

    private void updateResource(ListResource list, IGenericClient fhirStoreClient) {
        fhirStoreClient.update().resource(list).execute();
    }

    private Bundle searchListByCodeData(String system, String value, String date, IGenericClient fhirStoreClient) {
        Bundle bundle = fhirStoreClient
                .search()
                .forResource(ListResource.class)
                .where(ListResource.IDENTIFIER.exactly().systemAndValues(system, value))
                .and(ListResource.DATE.exactly().day(date))
                .returnBundle(Bundle.class)
                .execute();
        return bundle;
    }

    public static String getDateWithoutTimeUsingFormat(Date date)
            throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    @PostMapping(value = "api/fhir/Bundle", consumes = MediaType.APPLICATION_XML_VALUE)
    public void receiveFHIRXML(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving RR FHIR XML. Parsing...");

        Resource bundle = this.ctx.newXmlParser().parseResource(Bundle.class, body);

        logger.debug("Done parsing. Storing RR FHIR XML...");

        this.receiveFHIR(bundle, request);
    }

    @PostMapping(value = "api/fhir/Bundle", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void receiveFHIRJSON(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving RR FHIR JSON. Parsing...");

        Resource bundle = this.ctx.newJsonParser().parseResource(Bundle.class, body);
        logger.debug("Done parsing. Storing RR FHIR JSON...");

        this.receiveFHIR(bundle, request);
    }


    @PostMapping(value = "api/fhir/List", consumes = {MediaType.APPLICATION_XML_VALUE})
    public void getPatientIdentifierListXML(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving RR FHIR XML. Parsing...");

        ListResource list = this.ctx.newXmlParser().parseResource(ListResource.class, body);
        logger.debug("Done parsing. Storing RR FHIR XML...");
        this.receiveFHIR(list, request);
    }

    @PostMapping(value = "api/fhir/List", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void getPatientIdentifierListJSON(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving RR FHIR JSON. Parsing...");

        Resource bundle = this.ctx.newJsonParser().parseResource(Bundle.class, body);

        logger.debug("Done parsing. Storing RR FHIR JSON...");

        this.receiveFHIR(bundle, request);
    }

    @PostMapping(value = "api/cda", consumes = MediaType.APPLICATION_XML_VALUE)
    public void receiveCDA(@RequestBody() String xml, HttpServletRequest request) throws Exception {
        FHIRTransformer transformer = new FHIRTransformer();

        logger.debug("Receiving RR CDA XML. Converting to FHIR4 XML...");

        FHIRTransformResult result = transformer.cdaToFhir4(xml);

        if (!result.isSuccess()) {
            logger.error("Failed to transform RR CDA XML to FHIR4 XML!");
            List<String> messages = result.getMessages().stream().map(m -> m.getMessage()).collect(Collectors.toList());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, StringUtils.join(messages, "\r\n"));
        } else {
            logger.debug("Parsing FHIR XML Bundle...");

            Resource bundle = this.ctx.newXmlParser().parseResource(Bundle.class, result.getResult());

            logger.debug("Done parsing. Storing RR FHIR XML...");

            this.receiveFHIR(bundle, request);
        }
    }
}
