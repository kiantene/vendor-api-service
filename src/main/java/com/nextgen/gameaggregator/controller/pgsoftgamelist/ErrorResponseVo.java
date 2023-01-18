package com.nextgen.gameaggregator.controller.pgsoftgamelist;

import lombok.Data;

@Data
public class ErrorResponseVo {
    private Integer code;
    private String message;
    private String traceId;
}
