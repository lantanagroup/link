import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.PatientScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PatientScoopTest {
    private FhirContext ctx = FhirContext.forR4();
    private IParser parser = ctx.newJsonParser();
    private List<String> patientIdList = new ArrayList<>();
    private PatientData patientData = null;
    private IGenericClient targetFhirServer;
    private IGenericClient nandinaFhirServer;

    @Before
    public void setup() {
        patientIdList = new ArrayList<>();
        patientIdList.add("CER12742745");
        patientIdList.add("CER12742747");
        patientIdList.add("CER12742741");
        patientIdList.add("CER12742744");
        patientIdList.add("CER12742739");
        patientIdList.add("CER12742746");
        patientIdList.add("CER12742737");
        patientIdList.add("CER12742743");
        patientIdList.add("CER12742736");

        targetFhirServer = ctx.newRestfulGenericClient("https://nandina-fhir.lantanagroup.com/fhir");
        nandinaFhirServer = ctx.newRestfulGenericClient("https://nandina-fhir.lantanagroup.com/fhir");
    }

    /**
     * Test creating and populating patient scoop data
     */
    @Test
    @Ignore
    public void PatientScoopData() throws Exception {
        PatientScoop patientScoop = new PatientScoop(targetFhirServer, nandinaFhirServer, patientIdList);
        List<PatientData> patientData = patientScoop.getPatientData();
        Assert.assertTrue(patientScoop != null);
        Assert.assertTrue(patientData != null);
        Assert.assertTrue(patientData.size() == 9);
    }

    /**
     * Test posting the bundle to the fhir server. Commenting this out for now so that we don't actually post
     * every time someone runs mvn clean install
     */
    @Test
    @Ignore
    public void PostBundleToFhirServerTest() throws Exception {
        PatientScoop patientScoop = new PatientScoop(targetFhirServer, nandinaFhirServer, patientIdList);
        patientScoop.getPatientData().parallelStream().forEach(data -> {
            Bundle bundle = data.getBundleTransaction();
            IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/cqf-ruler-r4/fhir");
            Bundle resp = client.transaction().withBundle(bundle).execute();
        });
        Assert.assertTrue(patientScoop != null);
        Assert.assertTrue(patientScoop.getPatientData() != null);
    }
}
