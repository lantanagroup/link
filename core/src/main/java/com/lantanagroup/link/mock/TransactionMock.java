package com.lantanagroup.link.mock;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.mockito.ArgumentMatcher;

@Getter @Setter @AllArgsConstructor
public class TransactionMock {
  private ArgumentMatcher<Bundle> argumentMatcher;
  private Bundle responseBundle;
}
