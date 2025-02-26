package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataVo {

    @NotBlank(message = "url can not be blank")
    private String link;

    @JsonProperty("session_id")
    private String sessionId;

}
