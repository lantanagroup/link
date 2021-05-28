package com.lantanagroup.link.api.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
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
}
