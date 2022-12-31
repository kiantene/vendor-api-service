package com.nextgen.gameaggregator.operator.vo;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Data;

@Data
public class ResponseVo {
    private String traceId;
    private ResponseCodes.Status status;
    private String message;
}
