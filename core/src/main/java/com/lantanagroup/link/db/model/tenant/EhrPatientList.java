package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
}
