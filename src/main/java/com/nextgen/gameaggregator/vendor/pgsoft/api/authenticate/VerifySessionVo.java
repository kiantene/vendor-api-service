package com.nextgen.gameaggregator.vendor.pgsoft.api.authenticate;

import lombok.Data;

@Data
public class VerifySessionVo {
    private String playerName;
    private String nickname;
    private String currency;
}
