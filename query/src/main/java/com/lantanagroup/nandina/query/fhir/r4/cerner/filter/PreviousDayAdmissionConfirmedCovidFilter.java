package com.lantanagroup.nandina.query.fhir.r4.cerner.filter;

import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class PreviousDayAdmissionConfirmedCovidFilter extends Filter {
    Date reportDate;
    LocalDate previousDay;

    public PreviousDayAdmissionConfirmedCovidFilter(Date reportDate) {
        super();
        this.reportDate = reportDate;
        this.previousDay = null;
    }

    @Override
    public boolean runFilter(PatientData pd) {
        boolean confirmed = false;
        LocalDate encounterStart = null;
        LocalDate reportDate = LocalDate.parse(this.reportDate.toString());
        previousDay = reportDate.minusDays(1);

        if (null != pd.getPrimaryEncounter().getPeriod()) {
            if (null != pd.getPrimaryEncounter().getPeriod().getStart())
                encounterStart = LocalDate.ofInstant(pd.getPrimaryEncounter().getPeriod().getStart().toInstant(), ZoneId.systemDefault());
            if (encounterStart.equals(previousDay)) {
                confirmed = true;
            }
        }

        return confirmed;
    }
}
