package com.nextgen.gameaggregator.vendor.cpgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonMessageDto {

    @NotNull
    @JsonProperty("sub_uid")
    private Integer subUid;
}
