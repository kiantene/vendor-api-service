package com.nextgen.gameaggregator.operator.vo;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ResponseVo {
    @NotBlank
    private String traceId;
    @NotNull
    private ResponseCodes.Status status;
    private String message;
}
