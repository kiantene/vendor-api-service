package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoundMarker {

    private final ApiVersion version;
    private final Long firstSeen;

    @JsonCreator
    public RoundMarker(
            @JsonProperty("version") ApiVersion version,
            @JsonProperty("firstSeen") Long firstSeen) {
        this.version = version;
        this.firstSeen = firstSeen;
    }

}
