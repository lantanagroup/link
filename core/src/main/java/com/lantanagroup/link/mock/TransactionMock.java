package com.lantanagroup.link.mock;

import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.mockito.ArgumentMatcher;

@Getter @Setter
public class TransactionMock {
  private ArgumentMatcher<Bundle> argumentMatcher;
  private Bundle responseBundle;
  private ITransactionTyped<Bundle> transactionTyped;

  public TransactionMock(ArgumentMatcher<Bundle> argumentMatcher, Bundle responseBundle) {
    this.argumentMatcher = argumentMatcher;
    this.responseBundle = responseBundle;
  }
}
