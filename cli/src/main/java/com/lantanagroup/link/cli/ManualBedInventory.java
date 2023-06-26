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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@ShellComponent
public class ManualBedInventory extends BaseShellCommand {
    private static final Logger logger = LoggerFactory.getLogger(ManualBedInventory.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/y");
    private ManualBedInventoryConfig config;

    @ShellMethod(
            key = "manual-bed-inventory",
            value = "Manual way to update total # of beds & icu-beds.")
    public void execute(int totalBeds, int totalIcuBeds) throws Exception {
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
        inventory.getGroup().get(0).addPopulation(GetPopulation("numTotBedsOcc","numTotBedsOcc","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));
        inventory.getGroup().get(0).addPopulation(GetPopulation("numTotBedsAvail","numTotBedsAvail","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));
        inventory.getGroup().get(0).addPopulation(GetPopulation("numTotBeds","numTotBeds","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", totalBeds));

        // icu-beds
        inventory.addGroup(new MeasureReport.MeasureReportGroupComponent());
        inventory.getGroup().get(1).getCode().getCodingFirstRep().setCode("icu-beds");
        inventory.getGroup().get(1).addPopulation(GetPopulation("numICUBedsOcc","numICUBedsOcc","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));
        inventory.getGroup().get(1).addPopulation(GetPopulation("numICUBedsAvail","numICUBedsAvail","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));
        inventory.getGroup().get(1).addPopulation(GetPopulation("numICUBeds","numICUBeds","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", totalIcuBeds));

        // vents - TODO do I really need this?!?
        inventory.addGroup(new MeasureReport.MeasureReportGroupComponent());
        inventory.getGroup().get(2).getCode().getCodingFirstRep().setCode("vents");
        inventory.getGroup().get(2).addPopulation(GetPopulation("numVentUse","numVentUse","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));
        inventory.getGroup().get(2).addPopulation(GetPopulation("numVentAvail","numVentAvail","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));
        inventory.getGroup().get(2).addPopulation(GetPopulation("numVent","numVent","http://hl7.org/fhir/uv/saner/CodeSystem/MeasuredValues", 0));


        return inventory;
    }

    private MeasureReport.MeasureReportGroupPopulationComponent GetPopulation(String code, String display, String system, int count) {
        MeasureReport.MeasureReportGroupPopulationComponent population = new MeasureReport.MeasureReportGroupPopulationComponent();
        population.setCode(GetCodeableConcept(code, display, system));
        population.setCount(count);
        return population;
    }

    private CodeableConcept GetCodeableConcept(String code, String display, String system) {
        CodeableConcept cc = new CodeableConcept();
        Coding c = cc.addCoding();
        c.setCode(code);
        c.setDisplay(display);
        c.setSystem(system);

        return cc;
    }

    private void submit(MeasureReport report) throws Exception {
        // TODO - add some logging here
        ApiDataStoreConfig dataStoreConfig = config.getDataStore();
        FhirDataProvider fhirDataProvider = new FhirDataProvider(dataStoreConfig);
        Bundle updateBundle = new Bundle();
        updateBundle.setType(Bundle.BundleType.TRANSACTION);
        updateBundle.addEntry()
                .setResource(report)
                .setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("MeasureReport/" + config.getBedInventoryReportId()));
        Bundle response = fhirDataProvider.transaction(updateBundle);
    }
}
