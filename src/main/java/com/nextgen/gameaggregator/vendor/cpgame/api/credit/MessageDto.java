package com.nextgen.gameaggregator.vendor.cpgame.api.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonMessageDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto extends CommonMessageDto {

    @NotBlank
    @JsonProperty("game_id")
    @Size(max = 255)
    private String gameId;

    @NotNull
    @JsonProperty("bet_info")
    private BetInfoDto betInfo;
}
