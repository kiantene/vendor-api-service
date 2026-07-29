package com.nextgen.gameaggregator.game.launcher.vplus.member.create;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberCreateResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("msg")
    private String msg;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private MemberCreateDataResponse data;

    public boolean isSuccess() {
        return code != null && (code == 200 || code == 12);
    }
}