package com.lantanagroup.link;

import com.lantanagroup.link.config.thsa.THSAConfig;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.Date;

public class GenericXLSProcessor implements IDataProcessor {
  private static final Logger logger = LoggerFactory.getLogger(GenericXLSProcessor.class);

  @Autowired
  private THSAConfig thsaConfig;

  @Override
  public void process(byte[] dataContent, FhirDataProvider fhirDataProvider) {
    try(InputStream contentStream = new ByteArrayInputStream(dataContent)) {
      XSSFWorkbook workbook = new XSSFWorkbook(contentStream);
      XSSFSheet sheet = workbook.getSheetAt(0);
      MeasureReport measureReport = convert(sheet);
      measureReport.setId(this.thsaConfig.getDataMeasureReportId());

      Bundle updateBundle = new Bundle();
      updateBundle.setType(Bundle.BundleType.TRANSACTION);
      updateBundle.addEntry()
              .setResource(measureReport)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("MeasureReport/" + this.thsaConfig.getDataMeasureReportId()));
      Bundle response = fhirDataProvider.transaction(updateBundle);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  MeasureReport convert(XSSFSheet sheet) {

    MeasureReport measureReport = new MeasureReport();
    measureReport.setDate(new Date());
    MeasureReport.MeasureReportGroupComponent group = new MeasureReport.MeasureReportGroupComponent();
    Coding ventCoding = new Coding();
    ventCoding.setSystem("TODO");
    ventCoding.setCode("vents");
    group.setCode(new CodeableConcept(ventCoding));
    group.addPopulation(getGroupPop("totalAvail", sheet, 45, 3));
    group.addPopulation(getGroupPop("inUse", sheet, 45, 4));
    group.addPopulation(getGroupPop("currentAvail", sheet, 45, 5));
    measureReport.addGroup(group);
    return measureReport;
  }

  MeasureReport.MeasureReportGroupPopulationComponent getGroupPop(String type, XSSFSheet sheet, int row, int col) {
    MeasureReport.MeasureReportGroupPopulationComponent pop = new MeasureReport.MeasureReportGroupPopulationComponent();
    Coding coding = new Coding();
    coding.setSystem(Constants.MeasuredValues);
    coding.setCode(type);
    coding.setDisplay(type);
    pop.setCode(new CodeableConcept(coding));
    pop.setCount(Integer.parseInt(sheet.getRow(row).getCell(col).toString()));
    return pop;
  }
}
