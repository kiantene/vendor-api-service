package com.nextgen.gameaggregator.vendor.cpgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    @NotNull
    @JsonProperty("sub_uid")
    private Integer subUid;

    @JsonProperty("bet_id")
    @Size(max = 255)
    private String betId;

    @JsonProperty("game_id")
    @Size(max = 255)
    private String gameId;

    @JsonProperty("bet_info")
    private BetInfoDto betInfo;
}
