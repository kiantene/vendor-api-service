package com.nextgen.gameaggregator.game.launcher.vplus.member.login;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberLoginResponse {
    private Integer code;

    private String msg;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private MemberLoginDataResponse data;

    public boolean isSuccess() {
        return code != null && code == 200;
    }
}