package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PatientReportModel {
    String id;
    String name;
    String sex;
    String dateOfBirth;
    Boolean excluded;
}
