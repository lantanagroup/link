package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class ParklandSubmissionInfo {
    @NotEmpty
    private String submissionUrl;
    @NotEmpty
    private LinkOAuthConfig submissionAuth;
}
