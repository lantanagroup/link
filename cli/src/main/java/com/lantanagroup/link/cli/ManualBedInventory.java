package com.lantanagroup.link.cli;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IdentifierHelper;
import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import java.util.Date;

@ShellComponent
public class ManualBedInventory extends BaseShellCommand {
    private static final Logger logger = LoggerFactory.getLogger(ManualBedInventory.class);
    private ManualBedInventoryConfig config;

    @ShellMethod(
            key = "manual-bed-inventory",
            value = "Manual way to update total # of beds & icu-beds.")
    public void execute(int totalBeds, int totalIcuBeds) {
        // Create a CSV and just call the ParklandInventoryImportCommand?
        // Or just create the MeasureReport and submit it?

        config = applicationContext.getBean(ManualBedInventoryConfig.class);

        MeasureReport bedInventory = CreateMeasureReport(totalBeds, totalIcuBeds);

        submit(bedInventory);
    }

    private MeasureReport CreateMeasureReport(int totalBeds, int totalIcuBeds) {
        Date date = new Date();

        MeasureReport inventory = new MeasureReport();
        inventory.getMeta().addProfile(config.getProfileUrl());
        inventory.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
        inventory.setType(MeasureReport.MeasureReportType.SUMMARY);
        inventory.setMeasure(config.getMeasureUrl());
        inventory.getSubject().setIdentifier(IdentifierHelper.fromString(config.getSubjectIdentifier()));
        inventory.setDate(date);
        inventory.getPeriod()
                .setStart(date, TemporalPrecisionEnum.DAY)
                .setEnd(date, TemporalPrecisionEnum.DAY);

        // beds
        inventory.addGroup(new MeasureReport.MeasureReportGroupComponent());
        inventory.getGroup().get(0).getCode().getCodingFirstRep().setCode("beds");
        inventory.getGroup().get(0).addPopulation(GetPopulation("numTotBedsOcc","numTotBedsOcc", 0));
        inventory.getGroup().get(0).addPopulation(GetPopulation("numTotBedsAvail","numTotBedsAvail", 0));
        inventory.getGroup().get(0).addPopulation(GetPopulation("numTotBeds","numTotBeds", totalBeds));

        // icu-beds
        inventory.addGroup(new MeasureReport.MeasureReportGroupComponent());
        inventory.getGroup().get(1).getCode().getCodingFirstRep().setCode("icu-beds");
        inventory.getGroup().get(1).addPopulation(GetPopulation("numICUBedsOcc","numICUBedsOcc", 0));
        inventory.getGroup().get(1).addPopulation(GetPopulation("numICUBedsAvail","numICUBedsAvail", 0));
        inventory.getGroup().get(1).addPopulation(GetPopulation("numICUBeds","numICUBeds", totalIcuBeds));

        // vents - TODO do I really need this?!?
        inventory.addGroup(new MeasureReport.MeasureReportGroupComponent());
        inventory.getGroup().get(2).getCode().getCodingFirstRep().setCode("vents");
        inventory.getGroup().get(2).addPopulation(GetPopulation("numVentUse","numVentUse", 0));
        inventory.getGroup().get(2).addPopulation(GetPopulation("numVentAvail","numVentAvail", 0));
        inventory.getGroup().get(2).addPopulation(GetPopulation("numVent","numVent", 0));


        return inventory;
    }

    private MeasureReport.MeasureReportGroupPopulationComponent GetPopulation(String code, String display, int count) {
        MeasureReport.MeasureReportGroupPopulationComponent population = new MeasureReport.MeasureReportGroupPopulationComponent();
        population.setCode(GetCodeableConcept(code, display));
        population.setCount(count);
        return population;
    }

    private CodeableConcept GetCodeableConcept(String code, String display) {
        CodeableConcept cc = new CodeableConcept();
        Coding c = cc.addCoding();
        c.setCode(code);
        c.setDisplay(display);
        c.setSystem("https://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues");

        return cc;
    }

    private void submit(MeasureReport report) {

        ApiDataStoreConfig dataStoreConfig = config.getDataStore();
        FhirDataProvider fhirDataProvider = new FhirDataProvider(dataStoreConfig);
        String submissionUrl = String.format("MeasureReport/%s", config.getBedInventoryReportId());

        logger.info("Submitting MeasureReport to {}", submissionUrl);

        Bundle updateBundle = new Bundle();
        updateBundle.setType(Bundle.BundleType.TRANSACTION);
        updateBundle.addEntry()
                .setResource(report)
                .setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl(submissionUrl));

        fhirDataProvider.transaction(updateBundle);
    }
}
