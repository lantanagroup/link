package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientId {
  private String reference;
  private String identifier;

  public PatientId(String reference) {
    this.reference = reference;
  }

  private boolean referenceEquals(PatientId other) {
    if (this.reference == null && other.reference == null) {
      return true;
    }

    if (this.reference != null && other.reference != null) {
      return this.reference.equals(other.reference);
    }

    return false;
  }

  private boolean identifierEquals(PatientId other) {
    if (this.identifier == null && other.identifier == null) {
      return true;
    }

    if (this.identifier != null && other.identifier != null) {
      return this.identifier.equals(other.identifier);
    }

    return false;
  }

  @Override
  public boolean equals(Object object) {
    if (object != null && object instanceof PatientId) {
      PatientId other = (PatientId) object;

      if (this.referenceEquals(other) && this.identifierEquals(other)) {
        return true;
      }
    }

    return false;
  }
}
