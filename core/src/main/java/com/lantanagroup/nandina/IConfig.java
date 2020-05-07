package com.lantanagroup.nandina;

public interface IConfig {
    public String getFhirServerBase();
    public String getFhirServerUserName();
    public String getFhirServerPassword();
    public String getFhirServerBearerToken();

    public String getTerminologyCovidCodesValueSet();
    public String getTerminologyVentilatorCodesValueSet();

    public String getTerminologyVentilatorCodes();
    public String getTerminologyCovidCodes();

    public String getQueryHospitalized();
    public String getQueryEDOverflow();
}
