package com.nextgen.gameaggregator.game.launcher.topbet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberCreateResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("org")
    private Integer org;

    @JsonProperty("balance")
    private BigDecimal balance;

    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
