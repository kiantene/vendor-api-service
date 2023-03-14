package com.nextgen.gameaggregator.vendor.cq9.api.gamelist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.cq9.vo.NameSetVo;
import lombok.Data;

import java.util.List;

@Data
public class GameDetailsDto {
    @JsonProperty("gamehall")
    private String gameHall;
    @JsonProperty("gametype")
    private String gameType;
    @JsonProperty("gamecode")
    private String gameCode;
    @JsonProperty("gamename")
    private String gameName;
    @JsonProperty("gametech")
    private String gameTech;
    @JsonProperty("gameplat")
    private String gamePlat;
    private List<String> lang;
    private Boolean status;
    private Boolean maintain;
    @JsonProperty("nameset")
    private List<NameSetVo> nameSet;
}
