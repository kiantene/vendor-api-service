package com.nextgen.gameaggregator.vendor.jdb.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.checkerframework.checker.index.qual.Positive;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @NotBlank
    @Positive
    @JsonProperty("action")
    private Integer action;
    private String params;
}
