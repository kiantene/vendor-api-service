package com.nextgen.gameaggregator.game.launcher.vplus.member.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreateDataResponse {

    @JsonProperty("username")
    private String username;

    @JsonProperty("uid")
    private String uid;

}
