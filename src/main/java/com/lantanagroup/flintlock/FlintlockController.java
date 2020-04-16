package com.lantanagroup.flintlock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.*;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.lantanagroup.flintlock.client.ValueSetQueryClient;
import com.lantanagroup.flintlock.ecr.ElectronicCaseReport;
import com.lantanagroup.flintlock.model.ClientReportResponse;
import com.lantanagroup.flintlock.model.LocationResponse;
import com.lantanagroup.flintlock.model.QuestionnaireResponseSimple;
import com.lantanagroup.flintlock.model.SimplePosition;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FlintlockController {

  private static final Logger logger = LoggerFactory.getLogger(FlintlockController.class);
  public String conformanceServerBase = "https://flintlock-fhir.lantanagroup.com/fhir";
  public String clinicalDataServerBase = "http://hapi.fhir.org/baseR4";
  FhirContext ctx = FhirContext.forR4();
  IParser xmlParser = ctx.newXmlParser().setPrettyPrint(true);
  IParser jsonParser = ctx.newJsonParser().setPrettyPrint(true);
  ValueSetQueryClient vsClient;
  IGenericClient clinicalDataClient;
  String symptomsValueSetUrl = "http://flintlock-fhir.lantanagroup.com/fhir/ValueSet/symptoms";
  String dxtcSnomedValueSetUrl = "https://flintlock-fhir.lantanagroup.com/fhir/ValueSet/dxtc-snomed";
  String dxtcCoronavirusValueSetUrl = "https://flintlock-fhir.lantanagroup.com/fhir/ValueSet/dxtc-coronavirus";

  public FlintlockController() {
    this.vsClient = new ValueSetQueryClient(conformanceServerBase, clinicalDataServerBase);
    this.clinicalDataClient = this.ctx.newRestfulGenericClient(clinicalDataServerBase);
    //ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
  }

  public ValidationResult validate(IBaseResource resource) {
    ValidationResult result = null;
    FhirValidator validator = ctx.newValidator();
    IValidatorModule module1 = new SchemaBaseValidator(ctx);
    validator.registerValidatorModule(module1);
    IValidatorModule module2 = new SchematronBaseValidator(ctx);
    validator.registerValidatorModule(module2);
    result = validator.validateWithResult(resource);
    for (SingleValidationMessage next : result.getMessages()) {
      System.out.println(next.getLocationString() + " " + next.getMessage());
    }
    return result;
  }

  @GetMapping("google-api-key")
  public String getGoogleApiKey() {
    return Config.getInstance().googleApiKey;
  }

  @GetMapping(value = "patients", produces = "application/fhir+xml")
  public String patients() {
    ValueSet vs = vsClient.getValueSet(dxtcSnomedValueSetUrl);
    logger.info("Retrieved value set", vs.getUrl());
    List<Condition> resultList = vsClient.conditionCodeQuery(vs);
    Map<String, Patient> patientRefs = getUniquePatientReferences(resultList);
    Bundle b = new Bundle();
    b.setType(BundleType.COLLECTION);
    for (String key : patientRefs.keySet()) {
      Patient p = patientRefs.get(key);
      BundleEntryComponent entry = b.addEntry();
      entry.setFullUrl(clinicalDataServerBase + "/" + key);
      entry.setResource(p);
    }
    String parsedResource = xmlParser.encodeResourceToString(b);
    logger.info(parsedResource);
    return parsedResource;
  }


  @GetMapping(value = "case-report-bundle.json", produces = "application/fhir+json")
  public String reportJson() {
    Bundle b = getReportBundle();
    String parsedResource = jsonParser.encodeResourceToString(b);
    return parsedResource;
  }

  @GetMapping(value = "case-report-bundle.xml", produces = "application/fhir+xml")
  public String reportXml() {
    Bundle b = getReportBundle();
    String parsedResource = xmlParser.encodeResourceToString(b);
    return parsedResource;
  }

  @PostMapping(value = "convert", produces = "application/fhir+xml")
  public String reportXml(@RequestBody String content) {
    try {
      Bundle bundle = (Bundle) this.jsonParser.parseResource(content);
      return this.xmlParser.encodeResourceToString(bundle);
    } catch (Exception ex) {
      return content;
    }
  }

  @GetMapping(value = "report", produces = "application/fhir+xml")
  public String report() {
    Bundle b = getReportBundle();
    String parsedResource = xmlParser.encodeResourceToString(b);
    return parsedResource;
  }

  private HashMap<String, SimplePosition> getCachedGeoCoordinates() {
    if (!new File("coordinates.txt").exists()) {
      return new HashMap<String, SimplePosition>();
    }

    try {
      FileInputStream fs = new FileInputStream("coordinates.txt");
      ObjectInputStream ois = new ObjectInputStream(fs);
      HashMap<String, SimplePosition> coordinates = (HashMap) ois.readObject();
      ois.close();
      fs.close();
      return coordinates;
    } catch (Exception ex) {
      return new HashMap<String, SimplePosition>();
    }
  }

  private void setCachedGeoCoordinates(HashMap<String, SimplePosition> coordinates) {
    try {
      FileOutputStream fs = new FileOutputStream("coordinates.txt");
      ObjectOutputStream oos = new ObjectOutputStream(fs);
      oos.writeObject(coordinates);
      oos.close();
      fs.close();
    } catch (Exception ex) {
      // Do nothing
    }
  }

  @GetMapping("client-report")
  public ClientReportResponse getClientReport() {
    ClientReportResponse response = new ClientReportResponse();
    Bundle bundle = this.getReportBundle();
    response.setBundle(this.jsonParser.encodeResourceToString(bundle));
    List<SimplePosition> positions = this.getHeatMapData(bundle);
    response.setPositions(positions);
    return response;
  }

  private List<SimplePosition> getHeatMapData(Bundle bundle) {
    List<SimplePosition> positions = new ArrayList();
    GeoApiContext geoContext = new GeoApiContext.Builder()
      .apiKey(Config.getInstance().googleApiKey)
      .build();
    HashMap<String, SimplePosition> cached = this.getCachedGeoCoordinates();

    for (BundleEntryComponent entry : bundle.getEntry()) {
      Bundle ecr = (Bundle) entry.getResource();
      Patient patient = null;

      for (BundleEntryComponent ecrEntry : ecr.getEntry()) {
        if (ecrEntry.getResource().getResourceType().equals(ResourceType.Patient)) {
          patient = (Patient) ecrEntry.getResource();
          break;
        }
      }

      if (patient == null) {
        continue;
      }

      for (Address addr : patient.getAddress()) {
        if (addr.getCity() == null || addr.getCity().isEmpty() || addr.getState() == null || addr.getState().isEmpty()) {
          continue;   // ignore addresses that don't have a city and state
        }

        String address = addr.getCity() + " " + addr.getState();

        if (cached.containsKey(address)) {
          SimplePosition cachedPosition = cached.get(address);
          positions.add(cachedPosition);
          continue;
        }

        try {
          GeocodingResult[] results = GeocodingApi.geocode(geoContext, address).await();

          if (results != null && results.length > 0 && results[0].geometry != null && results[0].geometry.location != null) {
            SimplePosition position = new SimplePosition();
            position.setLatitude(results[0].geometry.location.lat);
            position.setLongitude(results[0].geometry.location.lng);
            cached.put(address, position);
            positions.add(position);
          }
        } catch (Exception ex) {
          System.err.println("Error GeoCoding address: " + ex.getMessage());
        }
      }
    }

    this.setCachedGeoCoordinates(cached);
    return positions;
  }

  private Bundle getReportBundle() {
    ValueSet vs = vsClient.getValueSet(dxtcCoronavirusValueSetUrl);
    logger.info("Retrieved value set", vs.getUrl());
    List<Condition> resultList = vsClient.conditionCodeQuery(vs);
    Map<String, Patient> patientRefs = getUniquePatientReferences(resultList);
    Bundle b = new Bundle();
    // b.setType(BundleType.TRANSACTION);
    b.setType(BundleType.COLLECTION);
    for (String key : patientRefs.keySet()) {
      logger.info("Building report for {}", key);
      Patient p = patientRefs.get(key);
      ElectronicCaseReport ecr = new ElectronicCaseReport(this.clinicalDataClient, p, null, null);
      Bundle ecrDoc = ecr.compile();
      logger.info("Created report {}", ecrDoc.getId());
      BundleEntryComponent entry = b.addEntry();
      entry.setFullUrl(ecrDoc.getId());
      entry.setResource(ecrDoc);
      if (b.getType().equals(BundleType.TRANSACTION)) {
        entry.getRequest().setMethod(HTTPVerb.PUT).setUrl("Bundle/");
      }
    }

    // TODO Maven dependency for HAPI FHIR Validation not working, figure out why
		/*
		ValidationResult result = validate(b);
        if (result.isSuccessful()) {
        	logger.info("Bundle is valid");
        } else {
        	logger.info("Bundle is not valid. Output may be incomplete.");
        }
		 */

    logger.info("Finished creating reports");
    return b;
  }

  @GetMapping(value = "test/{patientId}", produces = "application/xml")
  public String test(@PathVariable("patientId") String patientId) {
    Patient subject = (Patient) this.clinicalDataClient
      .read()
      .resource(Patient.class)
      .withId(patientId)
      .execute();
    ElectronicCaseReport ecr = new ElectronicCaseReport(this.clinicalDataClient, subject, null, null);
    Bundle ecrDoc = ecr.compile();
    IParser xmlParser = this.ctx.newXmlParser();
    return xmlParser.encodeResourceToString(ecrDoc);
  }

  @GetMapping("location")
  public List<LocationResponse> getLocations(@RequestParam(required = false) String search) throws UnsupportedEncodingException {
    String url = "Location?_summary=true&_count=10";

    if (search != null && !search.isEmpty()) {
      url += "&_content=" + URLEncoder.encode(search, "utf-8");
    }

    List<LocationResponse> response = new ArrayList();
    Bundle locationsBundle = this.clinicalDataClient.search()
      .byUrl(url)
      .returnBundle(Bundle.class)
      .execute();

    for (BundleEntryComponent entry : locationsBundle.getEntry()) {
      LocationResponse newLocResponse = new LocationResponse();
      Location loc = (Location) entry.getResource();
      String name = loc.getName();

      if (name == null || name.isEmpty()) name = "Unspecified Name";

      newLocResponse.setId(loc.getIdElement().getIdPart());
      newLocResponse.setDisplay(name);
      response.add(newLocResponse);
    }

    return response;
  }

  @GetMapping("questionnaire-response")
  public QuestionnaireResponseSimple getQuestionnaireResponse(@RequestParam(required = false) String overflowLocations) {
    QuestionnaireResponseSimple response = new QuestionnaireResponseSimple();
    String covidCodes = "441590008,651000146102,715882005,186747009,713084008,840539006,840544004";
    String deviceTypeCodes = "706172005,426160001,272189001,449071006,706173000,465703003,700657002,250870006,444932008,409025002,385857005";

    try {
      String url = String.format("Patient?_summary=true&_active=true&_has:Condition:patient:code=%s", covidCodes);
      Bundle hospitalizedBundle = this.clinicalDataClient.search()
        .byUrl(url)
        .returnBundle(Bundle.class)
        .execute();
      response.setHospitalized(hospitalizedBundle.getTotal());
    } catch (Exception ex) {
      System.err.println("Could not retrieve hospitalized count: " + ex.getMessage());
      ex.printStackTrace();
    }

    try {
      String url = String.format("Patient?_active=true&_has:Condition:patient:code=%s&_has:Device:type:patient=%s",
        covidCodes,
        deviceTypeCodes);
      Bundle hospAndVentilatedBundle = this.clinicalDataClient.search()
        .byUrl(url)
        .returnBundle(Bundle.class)
        .execute();
      response.setHospitalizedAndVentilated(hospAndVentilatedBundle.getTotal());
    } catch (Exception ex) {
      System.err.println("Could not retrieve hospitalized & ventilated count: " + ex.getMessage());
      ex.printStackTrace();
    }

    try {
      String url = String.format("Patient?_deceased=true&_has:Condition:patient:code=%s", covidCodes);
      Bundle deathsBundle = this.clinicalDataClient.search()
        .byUrl(url)
        .returnBundle(Bundle.class)
        .execute();
      response.setDeaths(deathsBundle.getTotal());
    } catch (Exception ex) {
      System.err.println("Could not retrieve deaths count: " + ex.getMessage());
      ex.printStackTrace();
    }

    if (overflowLocations != null && !overflowLocations.isEmpty()) {
      try {
        String url = String.format("Patient?_has:Condition:patient:code=%s&_has:Encounter:patient:location=%s",
          covidCodes,
          overflowLocations);
        Bundle edOverflowBundle = this.clinicalDataClient.search()
          .byUrl(url)
          .returnBundle(Bundle.class)
          .execute();
        response.setEdOverflow(edOverflowBundle.getTotal());
      } catch (Exception ex) {
        System.err.println("Could not retrieve ED/overflow count: " + ex.getMessage());
        ex.printStackTrace();
      }
    }

    return response;
  }

  private Map<String, Patient> getUniquePatientReferences(List<Condition> conditions) {
    HashMap<String, Patient> patients = new HashMap<String, Patient>();
    for (Condition c : conditions) {
      String key = c.getSubject().getReference();
      Patient p = clinicalDataClient.read().resource(Patient.class).withUrl(key).execute();
      patients.put(key, p);
    }
    return patients;
  }
}
