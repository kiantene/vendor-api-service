package com.nextgen.gameaggregator.vendor.cpgame.api.balance;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MessageDto {
    @NotBlank
    @Pattern(regexp = "^hog$") // must be "hog"
    @JsonProperty("game_key")
    private String gameKey;

    @NotNull
    @JsonProperty("sub_uid")
    private Long subUid;
}
