package com.lantanagroup.nandina.query.fhir.r4.cerner.filter;

import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class OnsetFilter extends Filter {
    Date reportDate;
    LocalDate previousDay;

    public OnsetFilter(Date reportDate) {
        super();
        this.reportDate = reportDate;
        this.previousDay = null;
    }

    public OnsetFilter(Date reportDate, LocalDate previousDay) {
        super();
        this.reportDate = reportDate;
        this.previousDay = previousDay;
    }

    @Override
    public boolean runFilter(PatientData pd) {
        boolean onset = false;
        LocalDate encounterStart = null;
        LocalDate encounterEnd = null;
        LocalDate reportDate = LocalDate.parse(this.reportDate.toString());

        if (null != pd.getPrimaryEncounter().getPeriod()) {
            if (null != pd.getPrimaryEncounter().getPeriod().getStart())
                encounterStart = LocalDate.ofInstant(pd.getPrimaryEncounter().getPeriod().getStart().toInstant(), ZoneId.systemDefault());
            if (null != pd.getPrimaryEncounter().getPeriod().getEnd())
                encounterEnd = LocalDate.ofInstant(pd.getPrimaryEncounter().getPeriod().getEnd().toInstant(), ZoneId.systemDefault());

            for (Bundle.BundleEntryComponent entry : pd.getConditions().getEntry()) {
                Condition condition = (Condition) entry.getResource();
                if (condition.hasOnsetDateTimeType()) {
                    LocalDate onsetDate = LocalDate.ofInstant(condition.getOnsetDateTimeType().getValue().toInstant(), ZoneId.systemDefault());
                    onset = onsetDuringEncounter(onsetDate, encounterStart, encounterEnd);
                    if (null != this.previousDay && !this.previousDay.equals(onsetDate)) {
                        onset = false;
                    }
                } else if (condition.hasOnsetPeriod()) {
                    LocalDate onsetPeriodStart = LocalDate.ofInstant(condition.getOnsetPeriod().getStartElement().getValue().toInstant(), ZoneId.systemDefault());
                    onset = onsetDuringEncounter(onsetPeriodStart, encounterStart, encounterEnd);
                    if (onset == false) {
                        LocalDate onsetPeriodEnd = LocalDate.ofInstant(condition.getOnsetPeriod().getEndElement().getValue().toInstant(), ZoneId.systemDefault());
                        onset = onsetDuringEncounter(onsetPeriodEnd, encounterStart, encounterEnd);
                    }
                }
            }
        }
        return onset;
    }

    /**
     * checks to see if patient has onset during the encounter after 14 or more days. This also checks to see if onset
     * happened after discharge in which case would not be a hospital onset.
     * @param onsetDate
     * @param encounterStartDate
     * @param encounterEndDate
     * @return
     */
    private boolean onsetDuringEncounter(LocalDate onsetDate, LocalDate encounterStartDate, LocalDate encounterEndDate) {
        boolean hospitalOnset = false;
        if (encounterStartDate != null) {
            LocalDate encounterStartPlus14 = encounterStartDate.plusDays(14);
            if (onsetDate.isAfter(encounterStartPlus14)) {
                hospitalOnset = true;
                if (encounterEndDate != null) {
                    if (onsetDate.isAfter(encounterEndDate)) {
                        // onset after discharge
                        hospitalOnset = false;
                    }
                }
            }
        }
        return hospitalOnset;
    }
}
