package com.nextgen.gameaggregator.vendor.bglive.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorVo {
    private Integer code;
    private String message;
    private String reason;
}
