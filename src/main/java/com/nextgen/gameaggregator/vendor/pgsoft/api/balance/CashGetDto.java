package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashGetDto extends CommonDto {
    @NotBlank
    @Size(min = 1, max = 50)
    @JsonProperty("player_name")
    private String playerName;

    //* Below are not mandatory
    @JsonProperty("operator_player_session")
    private String operatorPlayerSession;
    @JsonProperty("game_id")
    private Integer gameId;
}
