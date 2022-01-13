package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.ResourceIdChanger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

public class FhirHelperTests {

  private FhirDataProvider fhirDataProviderTest;
  private FhirHelper fhirHelperTest;
  private FhirContext ctxTest;
  private Bundle bundleTest;

  @Test
  public void getNameTest() {
    HumanName name1 = new HumanName().setFamily("Sombody").addGiven("Joe");
    HumanName name2 = new HumanName().addGiven("Joe Sombody");
    HumanName name3 = new HumanName().setFamily("Joe Sombody");
    HumanName name4 = new HumanName().setText("Joe Sombody");
    HumanName name5 = new HumanName();

    String actual1 = FhirHelper.getName(Arrays.asList(name1));
    String actual2 = FhirHelper.getName(Arrays.asList(name2));
    String actual3 = FhirHelper.getName(Arrays.asList(name3));
    String actual4 = FhirHelper.getName(Arrays.asList(name4));
    String actual5 = FhirHelper.getName(Arrays.asList(name5));
    String actual6 = FhirHelper.getName(Arrays.asList());

    Assert.assertEquals(actual1, "Joe Sombody");
    Assert.assertEquals(actual2, "Joe Sombody");
    Assert.assertEquals(actual3, "Joe Sombody");
    Assert.assertEquals(actual4, "Joe Sombody");
    Assert.assertEquals(actual5, "Unknown");
    Assert.assertEquals(actual6, "Unknown");
  }

  @Test
  public void recordAuditEventTest(){

  }

  @Test
  public void bundleMeasureReportTest(){

    
  }

  @Test
  public void getAllPagesTest(){
    setup();
    Resource resourceTest = mock(Resource.class);
    bundleTest.addEntry().setResource(resourceTest);
    List<IBaseResource> bundles = FhirHelper.getAllPages(bundleTest, fhirDataProviderTest, ctxTest);
    Assert.assertEquals(1, bundles.size());
  }

  private void setup(){

    fhirDataProviderTest = mock(FhirDataProvider.class);
    fhirHelperTest = new FhirHelper();
    ctxTest = FhirContext.forR4();
    bundleTest = new Bundle();
  }
}
