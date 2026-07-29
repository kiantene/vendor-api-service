package com.nextgen.gameaggregator.game.launcher.vplus.member.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberLoginDataResponse {
    @JsonProperty("username")
    private String username;

    @JsonProperty("token")
    private String token;
}
