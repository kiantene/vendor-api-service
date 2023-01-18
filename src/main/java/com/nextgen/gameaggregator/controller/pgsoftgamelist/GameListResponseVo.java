package com.nextgen.gameaggregator.controller.pgsoftgamelist;

import lombok.Data;

import java.util.List;

@Data
public class GameListResponseVo {
    private List<GameResponseVo> data;
    private ErrorResponseVo error;
}
