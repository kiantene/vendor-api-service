package com.nextgen.gameaggregator.game.launcher.groove;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {

    private String accountid;

    private String country;

    private Integer nogsgameid;

    private String nogslang;

    private String nogsmode;

    private String nogsoperatorid;

    private String nogscurrency;

    private String sessionid;

    private String homeurl;

    private String license;

    private Boolean is_test_account;

    private String device_type;
}
