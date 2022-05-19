package com.lantanagroup.link;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FhirDataProviderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void deleteResource() {
    FhirDataProvider fhirDataProvider = new FhirDataProvider("https://dev-fhir.nhsnlink.org/fhir");
    String resourceType = "Medication";
    String resourceId = "medicationTest1";
    Resource testResource = new Medication();
    testResource.setId(resourceId);

    IGenericClient client = fhirDataProvider.getClient();
    IUpdate update = client.update();
    IUpdateTyped updateTyped = update.resource(testResource);
    updateTyped.execute();

    IClientExecutable clientExecutable = getTestResource(client, resourceType, resourceId);
    Resource retrievedResource = (Resource) clientExecutable.execute();

    Assert.assertNotNull(retrievedResource);
    fhirDataProvider.deleteResource(resourceType, resourceId, true);

    IClientExecutable clientExecutable2 = getTestResource(client, resourceType, resourceId);

    thrown.expect(ResourceGoneException.class);
    Assert.assertNull(clientExecutable2.execute());
  }

  private IClientExecutable getTestResource(IGenericClient client, String type, String id) {
    IRead read = client.read();
    IReadTyped readTyped = read.resource(type);
    IReadExecutable readExecutable = readTyped.withId(id);
    IClientExecutable clientExecutable = readExecutable.cacheControl(new CacheControlDirective().setNoCache(true));
    return clientExecutable;
  }
}