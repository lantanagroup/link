package com.lantanagroup.link;

import com.lantanagroup.link.config.parkland.ParklandConfig;
import com.lantanagroup.link.config.thsa.THSAConfig;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.Iterator;

public class GenericXLSProcessor implements IDataProcessor {
  private static final Logger logger = LoggerFactory.getLogger(GenericXLSProcessor.class);

  @Autowired
  private ParklandConfig parklandConfig;

  @Autowired
  private THSAConfig thsaConfig;

  @Override
  public void process(byte[] dataContent, FhirDataProvider fhirDataProvider) {
    MeasureReport measureReport = new MeasureReport();

    Measure measure = new Measure();
    try(InputStream measureStream = getClass().getClassLoader().getResourceAsStream("ParklandMasterAggregate.xml")) {
      measure = FhirContextProvider.getFhirContext().newXmlParser().parseResource(Measure.class, measureStream);
    } catch(IOException ex){
      logger.error("Error retrieving measure in Parkland data processor: " + ex.getMessage());
    }

    try(InputStream contentStream = new ByteArrayInputStream(dataContent)) {

      XSSFWorkbook workbook = new XSSFWorkbook(contentStream);
      for(int i = workbook.getNumberOfSheets() -1; i > -1; i--) {
        if(isSheetEmpty(workbook.getSheetAt(i))) {
          // in case of blank sheets at the end, do nothing and pass on
          continue;
        }
        XSSFSheet sheet = workbook.getSheetAt(i);
        measureReport = convert(sheet, measure);

        measureReport.setId(this.thsaConfig.getDataMeasureReportId());

        measureReport.getGroup().clear();
        measureReport.getGroup().addAll(this.parklandConfig.getGroup());



        Bundle updateBundle = new Bundle();
        updateBundle.setType(Bundle.BundleType.TRANSACTION);
        updateBundle.addEntry()
                .setResource(measureReport)
                .setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("MeasureReport/" + this.thsaConfig.getDataMeasureReportId()));
        Bundle response = fhirDataProvider.transaction(updateBundle);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  MeasureReport convert(XSSFSheet sheet, Measure measure) {
    // TODO get measure report from xlsx sheet


    return new MeasureReport();
  }

  boolean isSheetEmpty(XSSFSheet sheet){
    Iterator rows = sheet.rowIterator();
    while (rows.hasNext()) {
      XSSFRow row = (XSSFRow) rows.next();
      Iterator cells = row.cellIterator();
      while (cells.hasNext()) {
        XSSFCell cell = (XSSFCell) cells.next();
        if(!cell.getStringCellValue().isEmpty()){
          return true;
        }
      }
    }
    return false;
  }
}
