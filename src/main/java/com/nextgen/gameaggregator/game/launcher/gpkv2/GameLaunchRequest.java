package com.nextgen.gameaggregator.game.launcher.gpkv2;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {
    private String api_token;
    private String cid;
    private String player_id;
    private Integer provider;
    private String game;
    private String nickname;
    private String currency;
    private String player_token;
    private String balance;
    private String ip;
    private String country;
    private String return_url;
    // optional: leave the key out rather than send null
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String lang;
    private String client_type;
}
