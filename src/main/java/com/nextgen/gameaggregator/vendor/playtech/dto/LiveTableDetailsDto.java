package com.nextgen.gameaggregator.vendor.playtech.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveTableDetailsDto {

    @Size(max = 255)
    @JsonProperty("dealerName")
    private String dealerName;

    @Size(max = 128)
    @JsonProperty("launchAlias")
    private String launchAlias;

    @Size(max = 128)
    @JsonProperty("tableId")
    private String tableId;

    @Size(max = 128)
    @JsonProperty("tableName")
    private String tableName;
}
