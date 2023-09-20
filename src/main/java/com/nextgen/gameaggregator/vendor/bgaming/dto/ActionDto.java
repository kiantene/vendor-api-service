package com.nextgen.gameaggregator.vendor.bgaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {
    @JsonProperty("action")
    private String action;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("action_id")
    private String actionId;
    @JsonProperty("original_action_id")
    private String originalActionId;
}
