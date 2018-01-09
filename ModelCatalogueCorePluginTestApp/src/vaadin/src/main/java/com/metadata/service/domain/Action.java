package com.metadata.service.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true, allowSetters = true)
public class Action {

    @JsonProperty("id")
    private int id;

    @JsonProperty("state")
    private State state;

    @JsonProperty("matchParameters")
    private MatchParameters matchParameters;
}
