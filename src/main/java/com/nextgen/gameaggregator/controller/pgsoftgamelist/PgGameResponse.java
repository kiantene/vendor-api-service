package com.nextgen.gameaggregator.controller.pgsoftgamelist;

import lombok.Data;

@Data
public class PgGameResponse {
    private Integer gameId;
    private String gameName;
    private String gameCode;
    private Integer status;
    private Integer releaseStatus;
    private Boolean isSupportFreeGame;
    private Integer category;
}
