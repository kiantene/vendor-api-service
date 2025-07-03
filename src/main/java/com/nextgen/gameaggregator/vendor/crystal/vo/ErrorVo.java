package com.nextgen.gameaggregator.vendor.crystal.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorVo {
    private Integer code;
    private String message;

    public ErrorVo(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}