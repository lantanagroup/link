package com.lantanagroup.link.db.model.tenant;

import com.lantanagroup.link.ReportingPeriodMethods;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class EhrPatientList {
  @NotNull
  @Size(min = 1)
  private List<@NotBlank String> listId;

  @NotNull
  @Size(min = 1)
  private List<@NotBlank String> measureId;

  private ReportingPeriodMethods reportingPeriodMethod = ReportingPeriodMethods.CurrentMonth;
}
