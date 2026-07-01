package com.nextgen.gameaggregator.game.launcher.mtlive.member.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberCreateRequest {

    @JsonProperty("system_code")
    private String systemCode;

    @JsonProperty("web_id")
    private String webId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("currency")
    private String currency;
}
