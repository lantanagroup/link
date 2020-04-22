package com.lantanagroup.flintlock;

public interface IConfig {
    public String getFhirServerBase();
    public String getFhirServerUserName();
    public String getFhirServerPassword();
    public String getFhirServerBearerToken();

    public String getTerminologyCovidCodes();
    public String getTerminologyDeviceTypeCodes();

    public String getQueryHospitalized();
}
