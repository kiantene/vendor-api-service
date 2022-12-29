package com.nextgen.gameaggregator.vendor.pgsoft.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifySessionDto extends CommonDto {
    @NotBlank
    @JsonProperty("operator_player_session")
    private String operatorPlayerSession;

    //* Below are not mandatory

    private String ip;
    @JsonProperty("custom_parameter")
    private String customParameter;
    @JsonProperty("game_id")
    private Integer gameId;

}


