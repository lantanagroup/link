package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;

import java.util.List;

public interface IPatientIdProvider {
  List<String> getPatientIdentifiers(ReportCriteria criteria, ReportContext context, ApiConfig config);
}
