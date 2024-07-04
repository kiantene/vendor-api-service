package com.nextgen.gameaggregator.vendor.cpgame.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$")
    @JsonProperty("bet_id")
    private String betId;


}
