package com.lantanagroup.link.thsa;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.config.thsa.THSAConfig;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Date;

@Component
public class GenericXLSXProcessor implements IDataProcessor {

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
    group.addPopulation(getGroupPop(sheet.getRow(2).getCell(3).toString(), sheet, 45, 3));
    group.addPopulation(getGroupPop(sheet.getRow(2).getCell(4).toString(), sheet, 45, 4));
    group.addPopulation(getGroupPop(sheet.getRow(2).getCell(5).toString(), sheet, 45, 5));
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
