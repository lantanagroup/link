package com.lantanagroup.nandina.query.fhir.r4.cerner.filter;

import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;

import java.time.LocalDate;
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
        boolean onset = true;
        LocalDate reportDate = LocalDate.parse(this.reportDate.toString());

        return onset;
    }
}
