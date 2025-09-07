package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AgentMeta {
    @JsonProperty("agentId")
    private Integer agentId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("gameCode")
    private String gameCode;

    @JsonProperty("currency")
    private String currency;
}
