package com.lantanagroup.link.cli;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class ParklandSubmissionInfo {
    @NotEmpty
    private String submissionUrl;
    @NotEmpty
    private AuthConfig submissionAuth;
}
