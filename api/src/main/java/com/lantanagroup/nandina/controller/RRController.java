package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.fhir.transform.FHIRTransformResult;
import com.lantanagroup.fhir.transform.FHIRTransformer;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reportability Response Controller
 */
@RestController
public class RRController extends BaseController {
  private FhirContext ctx = FhirContext.forR4();
  private static final Logger logger = LoggerFactory.getLogger(RRController.class);

  private void receiveFHIR(Resource resource, HttpServletRequest request) throws Exception {
    IGenericClient fhirStoreClient = this.getFhirStoreClient(null, request);

    if (resource.hasId()) {
      resource.setId((String) null);
    }

    MethodOutcome outcome = fhirStoreClient.create().resource(resource).execute();

    if (!outcome.getCreated() && outcome.getResource() != null) {
      logger.error("Failed to store/create FHIR Bundle");
    } else {
      logger.debug("Stored FHIR Bundle with new ID of " + outcome.getResource().getIdElement().getIdPart());
    }
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
