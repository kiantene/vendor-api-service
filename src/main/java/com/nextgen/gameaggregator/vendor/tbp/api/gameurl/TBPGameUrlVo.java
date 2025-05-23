package com.nextgen.gameaggregator.vendor.tbp.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TBPGameUrlVo implements GameUrlVo {
    @NotBlank
    @JsonProperty("SessionId")
    private String sessionId;

    private String gameUrl;
}
