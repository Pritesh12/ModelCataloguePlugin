package com.metadata.service.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true, allowSetters = true)
public class Source {

    @JsonProperty("sourceId")
    private int sourceId;

    @JsonProperty("sourceClassifiedName")
    private String sourceClassifiedName;
}
