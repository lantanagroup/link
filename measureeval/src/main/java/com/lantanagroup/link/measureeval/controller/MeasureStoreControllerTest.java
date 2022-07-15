package com.lantanagroup.link.measureeval.controller;

import com.lantanagroup.link.measureeval.config.MeasureEvalConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class MeasureStoreControllerTest {

  @Test
  public void storeMeasure() {
    Bundle measureBundle = new Bundle();
    measureBundle.setId("UnitTest");
    MeasureEvalConfig config = new MeasureEvalConfig();
    config.setMeasuresPath("./measures");
    MeasureStoreController measureStoreController = new MeasureStoreController();
    measureStoreController.setConfig(config);
    measureStoreController.storeMeasure(measureBundle);
    Assert.assertTrue(Files.exists(Paths.get(config.getMeasuresPath() + File.separator + measureBundle.getId() + ".xml")));
  }
}