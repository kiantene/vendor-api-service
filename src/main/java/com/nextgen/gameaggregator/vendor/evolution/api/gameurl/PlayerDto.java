package com.nextgen.gameaggregator.vendor.evolution.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDto {
    private String id; // player username
    private Boolean update; // set False if player data is relevant for current session only
    private String firstName; // player username
    private String lastName; // player username
    private String nickname; // set null, will let user input their nickname on first login
    private String country;
    private String language;
    private String currency;
    private PlayerSessionDto session;
    private PlayerGroupDto group;
}
