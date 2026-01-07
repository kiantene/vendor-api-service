package com.nextgen.gameaggregator.vendor.endorphina.api.authenticate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticateResponse {

    private String player;
    private String currency;
    private String game;

}