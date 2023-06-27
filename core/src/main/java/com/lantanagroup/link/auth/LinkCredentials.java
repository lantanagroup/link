package com.lantanagroup.link.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirHelper;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
public class LinkCredentials {
    DecodedJWT jwt;
    Practitioner practitioner;

    public LinkCredentials(DecodedJWT jwt) {
        this.jwt = jwt;
        this.practitioner = FhirHelper.toPractitioner(jwt);
    }
}
