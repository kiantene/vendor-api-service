package com.nextgen.gameaggregator.vendor.evolutionlive.api.authentication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationDto {
    private String uuid;
    private PlayerDto player;
    private ConfigDto config;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class PlayerDto {
    private String id;
    private Boolean update;
    private String firstName;
    private String lastName;
    private String country;
    private String nickname;
    private String currency;
    private PlayerSessionDto session;
    private PlayerGroupDto group;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class PlayerSessionDto {
    private String id;
    private String ip;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class PlayerGroupDto {
    private String id;
    private String action;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigDto {
    private ConfigGameDto game;
    private ConfigChannelDto channel;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigGameDto {
    private String category;
    @JsonProperty("interface")
    private String game_interface;
    private GameTableDto table;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class GameTableDto {
    private String id;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class ConfigChannelDto {
    private Boolean wrapped;
    private Boolean mobile;
}