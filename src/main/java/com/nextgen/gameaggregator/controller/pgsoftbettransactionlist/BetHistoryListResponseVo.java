package com.nextgen.gameaggregator.controller.pgsoftbettransactionlist;

import com.nextgen.gameaggregator.controller.pgsoftgamelist.ErrorResponseVo;
import lombok.Data;

import java.util.List;

@Data
public class BetHistoryListResponseVo {
    private List<BetHistoryResponseVo> data;
    private ErrorResponseVo error;
}
