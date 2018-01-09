package com.metadata.service.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true, allowSetters = true)
public class MatchParameters {
    @JsonProperty("dest")
    private Dest dest;

    @JsonProperty("source")
    private Source source;

    @JsonProperty("matchScore")
    private float match;
}
