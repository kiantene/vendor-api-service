package com.nextgen.gameaggregator.game.launcher.saba;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MemberCreateResponse {
    public static final int SUCCESS = 0;
    public static final int DUPLICATE_MEMBER = 6;

    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("message")
    private String message;

    public boolean isSuccess() {
        return errorCode != null && (errorCode == SUCCESS || errorCode == DUPLICATE_MEMBER);
    }
}
