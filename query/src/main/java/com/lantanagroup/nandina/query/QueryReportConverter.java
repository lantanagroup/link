package com.lantanagroup.nandina.query;

import com.lantanagroup.nandina.QueryReport;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QueryReportConverter {
  private static String getCellValue(String data) {
    if (data != null) {
      String fixed = data.replace("\"", "\"\"");

      if (data.indexOf(" ") >= 0) {
        return "\"" + fixed + "\"";
      } else {
        return fixed;
      }
    }

    return "";
  }

  public static String getUnique(QueryReport report) {
    if (report.getAnswer("patients") == null) return "";

    List<HashMap<String, String>> patients = (List<HashMap<String, String>>) report.getAnswer("patients");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("DateCollected,Facility_ID,Census_ID,Patient_ID,Admit_Date,Discharge_Date,Patient_age,Patient_sex,Patient_race,Patient_ethnicity,Chief_complaint,Primary_dx,Patient_location,Disposition");

    patients.stream().forEach(pd -> {
      List<String> cells = new ArrayList<>();
      cells.add("");
      cells.add(QueryReportConverter.getCellValue(pd.get("facilityId")));
      cells.add(QueryReportConverter.getCellValue(pd.get("censusId")));
      cells.add(QueryReportConverter.getCellValue(pd.get("patientId")));
      cells.add(QueryReportConverter.getCellValue(pd.get("admitDate")));
      cells.add(QueryReportConverter.getCellValue(pd.get("dischargeDate")));
      cells.add(QueryReportConverter.getCellValue(pd.get("age")));
      cells.add(QueryReportConverter.getCellValue(pd.get("sex")));
      cells.add(QueryReportConverter.getCellValue(pd.get("race")));
      cells.add(QueryReportConverter.getCellValue(pd.get("ethnicity")));
      cells.add(QueryReportConverter.getCellValue(pd.get("chiefComplaint")));
      cells.add(QueryReportConverter.getCellValue(pd.get("primaryDx")));
      cells.add(QueryReportConverter.getCellValue(pd.get("location")));
      cells.add(QueryReportConverter.getCellValue(pd.get("disposition")));
      pw.println(StringUtils.join(cells, ","));
    });

    return sw.toString();
  }

  public static String getMeds(QueryReport report) {
    if (report.getAnswer("meds") == null) return "";

    List<HashMap<String, String>> meds = (List<HashMap<String, String>>) report.getAnswer("meds");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Patient_ID,Medication_Name,Medication_Code,Medication_Dose,Medication_Route,Medication_Start,Medication_Stop");

    meds.stream().forEach(med -> {
      List<String> cells = new ArrayList<>();
      cells.add("");
      cells.add(QueryReportConverter.getCellValue(med.get("patientId")));
      cells.add(QueryReportConverter.getCellValue(med.get("name")));
      cells.add(QueryReportConverter.getCellValue(med.get("code")));
      cells.add(QueryReportConverter.getCellValue(med.get("dose")));
      cells.add(QueryReportConverter.getCellValue(med.get("route")));
      cells.add(QueryReportConverter.getCellValue(med.get("start")));
      cells.add(QueryReportConverter.getCellValue(med.get("end")));
      pw.println(StringUtils.join(cells, ","));
    });

    return sw.toString();
  }

  public static String getDx(QueryReport report) {
    if (report.getAnswer("dx") == null) return "";

    List<HashMap<String, String>> diagnostics = (List<HashMap<String, String>>) report.getAnswer("dx");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Patient_ID,Other_dx");

    diagnostics.stream().forEach(dx -> {
      List<String> cells = new ArrayList<>();
      cells.add("");
      cells.add(QueryReportConverter.getCellValue(dx.get("patientId")));
      cells.add(QueryReportConverter.getCellValue(dx.get("code")));
      pw.println(StringUtils.join(cells, ","));
    });

    return sw.toString();
  }

  public static String getLabs(QueryReport report) {
    if (report.getAnswer("labs") == null) return "";

    List<HashMap<String, String>> labs = (List<HashMap<String, String>>) report.getAnswer("labs");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Patient_ID,SARSCOV_Lab_Order,SARSCOV_Lab_DateTime,SARSCOV_Lab_Result");

    labs.stream().forEach(lab -> {
      List<String> cells = new ArrayList<>();
      cells.add("");
      cells.add(QueryReportConverter.getCellValue(lab.get("patientId")));
      cells.add(QueryReportConverter.getCellValue(lab.get("order")));
      cells.add(QueryReportConverter.getCellValue(lab.get("date")));
      cells.add(QueryReportConverter.getCellValue(lab.get("result")));
      pw.println(StringUtils.join(cells, ","));
    });

    return sw.toString();
  }
}
