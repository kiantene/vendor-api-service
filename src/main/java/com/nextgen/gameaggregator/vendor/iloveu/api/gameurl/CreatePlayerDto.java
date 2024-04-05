package com.nextgen.gameaggregator.vendor.iloveu.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.iloveu.dto.DataDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePlayerDto {

    @JsonProperty("code")
    private String code;

    @JsonProperty("data")
    private DataDto dataDto;
}
