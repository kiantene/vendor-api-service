package com.nextgen.gameaggregator.game.launcher.vplus.member.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberLoginRequest {

    private String appId;

    private String timestamp;

    private String sign;

    private String username;
}