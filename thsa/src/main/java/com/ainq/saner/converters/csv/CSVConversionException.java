package com.ainq.saner.converters.csv;

import org.hl7.fhir.r4.model.CodeableConcept;

public class CSVConversionException extends Exception {
    private static final long serialVersionUID = 1L;
    private final CodeableConcept target;
    private CSVConversionException(String message) {
        super(message);
        target = null;
    }
    private CSVConversionException(Exception cause) {
        super(cause);
        target = null;
    }
    private CSVConversionException(String message, Exception cause) {
        super(message, cause);
        target = null;
    }
    public CSVConversionException(String message, CodeableConcept target) {
        super(message);
        this.target = target;
    }
    private CSVConversionException(Exception cause, CodeableConcept target) {
        super(cause);
        this.target = target;
    }
    private CSVConversionException(String message, Exception cause, CodeableConcept target) {
        super(message, cause);
        this.target = target;
    }
    public CodeableConcept getTarget() {
        return target;
    }
}