package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpungeResourcesToDelete {
    private String resourceType;
    private String[] resourceIdentifiers;
}
