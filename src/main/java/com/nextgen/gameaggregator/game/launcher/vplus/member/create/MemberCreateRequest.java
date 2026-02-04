package com.nextgen.gameaggregator.game.launcher.vplus.member.create;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberCreateRequest {

    private String appId;

    private String timestamp;

    private String sign;

    private String username;
}
