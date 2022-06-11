package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.MeasureReport;

import java.text.ParseException;
import java.util.List;

public interface IReportAggregator {
  MeasureReport generate(ReportCriteria criteria, ReportContext context, List<MeasureReport> measureReports) throws ParseException;
}
