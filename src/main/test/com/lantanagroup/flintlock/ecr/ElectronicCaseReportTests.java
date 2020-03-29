package com.lantanagroup.flintlock.ecr;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.Assert;
import org.junit.Test;

public class ElectronicCaseReportTests {

    @Test
    public void testNewElectronicCaseReport() {
        ElectronicCaseReport ecr = new ElectronicCaseReport();
        Bundle doc = ecr.compile();

        Assert.assertNotNull(doc);
        Assert.assertEquals(1, doc.getTotal());
        Assert.assertEquals(1, doc.getEntry().size());
    }
}