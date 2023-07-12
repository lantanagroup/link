package com.lantanagroup.link.config.datastore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataStoreDaoConfig {
    @Getter
    private Boolean autoCreatePlaceholderReferenceTargets;

    @Getter
    private Boolean enforceReferentialIntegrityOnDelete;

    @Getter
    private Boolean enforceReferentialIntegrityOnWrite;

    @Getter
    private Boolean allowContainsSearches;

    @Getter
    private Integer fetchSizeDefaultMaximum;

    @Getter
    private Long expireSearchResultsAfterMillis;

    @Getter
    private Boolean allowMultipleDelete;

    @Getter
    private Boolean expungeEnabled;

    @Getter
    private Boolean deleteExpungeEnabled;
}
