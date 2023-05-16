package com.lantanagroup.link.config.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class USCoreOtherResourceTypeConfig {
    private String resourceType;
    private Boolean supportsSearch = false;
    private Integer countPerSearch = 100;
}
