package com.nextgen.gameaggregator.vendor.cpgame.api.debit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    @NotBlank
    @Pattern(regexp = "^hog$") // must be "hog"
    @JsonProperty("game_key")
    private String gameKey;

    @NotNull
    @JsonProperty("sub_uid")
    private Integer subUid;

    @NotBlank
    @JsonProperty("game_id")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameId;

    @NotNull
    @JsonProperty("bet_info")
    private BetInfoDto betInfo;
}
