package com.nextgen.gameaggregator.game.launcher.lucky365.create;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreatePlayerRequest {

    private String sn;
    private String id;
    private String method;
    private String playerCode;
    private String playerName;
    private String signature;

}
