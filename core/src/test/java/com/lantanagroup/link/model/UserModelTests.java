package com.lantanagroup.link.model;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.Assert;
import org.junit.Test;

public class UserModelTests {
  @Test
  public void TestEmptyPractitioner() {
    Practitioner practitioner = new Practitioner();
    UserModel userModel = new UserModel(practitioner);
    Assert.assertEquals(null, userModel.getId());
    Assert.assertEquals(null, userModel.getName());
  }

  @Test
  public void TestPractitionerNoName() {
    Practitioner practitioner = new Practitioner();
    practitioner.setId(new IdType("http://somebase/fhir", "Practitioner", "test-practitioner", "2"));

    UserModel userModel = new UserModel(practitioner);
    Assert.assertEquals("test-practitioner", userModel.getId());
    Assert.assertEquals(null, userModel.getName());
  }

  @Test
  public void TestPractitionerWithName() {
    Practitioner practitioner = new Practitioner();
    HumanName name1 = practitioner.addName();
    name1.addGiven("Test");
    name1.addGiven("Middle");
    name1.setFamily("Last");

    UserModel userModel = new UserModel(practitioner);
    Assert.assertEquals(null, userModel.getId());
    Assert.assertEquals("Test Last", userModel.getName());

    name1.getGiven().clear();
    userModel = new UserModel(practitioner);
    Assert.assertEquals("Last", userModel.getName());

    name1.setFamily(null);
    name1.addGiven("Test");
    userModel = new UserModel(practitioner);
    Assert.assertEquals("Test", userModel.getName());
  }
}
