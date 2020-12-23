import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.util.BundleUtil;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.PatientScoop;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PatientScoopTest {
    private FhirContext ctx = FhirContext.forR4();
    private List<String> patientIdList = new ArrayList<>();
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

        targetFhirServer = ctx.newRestfulGenericClient("https://fhir.nandina.org/fhir");
        nandinaFhirServer = ctx.newRestfulGenericClient("https://fhir.nandina.org/fhir");
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

    @Test
    @Ignore
    public void loadBundleData() throws Exception {
        IGenericClient client = ctx.newRestfulGenericClient("https://fhir.nandina.org/fhir");
        Bundle response = client.fetchResourceFromUrl(Bundle.class, "https://fhir.nandina.org/fhir/Bundle");
        response.setType(Bundle.BundleType.TRANSACTION);
        response.getEntry().forEach(entry -> {
            entry.getRequest().setMethod(Bundle.HTTPVerb.POST);
        });

        IGenericClient cqfRulerClient = ctx.newRestfulGenericClient("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir");
        Bundle resp = cqfRulerClient.transaction().withBundle(response).execute();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));
    }

    @Test
    @Ignore
    public void loadPatientData() throws Exception {
        IGenericClient client = ctx.newRestfulGenericClient("https://fhir.nandina.org/fhir");
        Bundle patient = client.fetchResourceFromUrl(Bundle.class, "https://fhir.nandina.org/fhir/Patient");
        patient.setType(Bundle.BundleType.TRANSACTION);
        patient.getEntry().forEach(entry -> {
            entry.getRequest().setMethod(Bundle.HTTPVerb.POST);
        });

        IGenericClient cqfRulerClient = ctx.newRestfulGenericClient("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir");
        Bundle resp = cqfRulerClient.transaction().withBundle(patient).execute();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));
    }

    @Test
    @Ignore
    public void retrieveBundlesTest() throws Exception {
        IGenericClient cqfRulerClient = ctx.newRestfulGenericClient("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir");
        List<String> patientIds = new ArrayList<>();
        List<IBaseResource> bundles = new ArrayList<>();

        Bundle bundle = cqfRulerClient
                .search()
                .forResource(Bundle.class)
                .lastUpdated(new DateRangeParam("2020-12-06", "2020-12-07"))
                .returnBundle(Bundle.class)
                .execute();
        bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

        // Load the subsequent pages
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = cqfRulerClient
                    .loadPage()
                    .next(bundle)
                    .execute();
            bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
        }

        bundles.forEach(bundleResource -> {
            Bundle bundle1 = (Bundle) ctx.newJsonParser().parseResource(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundleResource));
            bundle1.getEntry().forEach(entry -> {
                if (entry.getResource().getResourceType().equals(ResourceType.Patient)) {
                    patientIds.add(entry.getResource().getIdElement().getIdPart());
                }
            });
        });
        System.out.println("Loaded " + patientIds.size() + " patients!");
    }
}
