package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDto {
    private String action;
    private String action_id;
    private String handler;
    private String version;
    private String game_id;
    private String absolute_name;
    private String mobile;

}
