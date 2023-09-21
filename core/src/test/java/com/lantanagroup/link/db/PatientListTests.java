package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.PatientId;
import com.lantanagroup.link.db.model.PatientList;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PatientListTests {
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Test
  public void testMerge() throws ParseException {
    PatientList patientList1 = new PatientList();
    patientList1.setLastUpdated(dateFormat.parse("2023-03-25 04:30:01"));
    patientList1.getPatients().add(PatientId.createFromReference("test1"));

    PatientList patientList2 = new PatientList();
    patientList2.setLastUpdated(dateFormat.parse("2023-03-26 02:30:01"));
    patientList2.getPatients().add(PatientId.createFromReference("test1"));
    patientList2.getPatients().add(PatientId.createFromReference("test2"));

    patientList1.merge(patientList2);

    Assert.assertEquals(2, patientList1.getPatients().size());
    Assert.assertEquals("test1", patientList1.getPatients().get(0).getReference());
    Assert.assertEquals("test2", patientList1.getPatients().get(1).getReference());
    Assert.assertEquals("2023-03-26 02:30:01", dateFormat.format(patientList1.getLastUpdated()));
  }
}
