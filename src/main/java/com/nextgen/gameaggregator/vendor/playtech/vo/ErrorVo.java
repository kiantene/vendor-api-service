package com.nextgen.gameaggregator.vendor.playtech.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.playtech.constant.ResponseCodes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorVo {

    private String code;

    public static ErrorVo from(ResponseCodes responseCode) {
        ErrorVo error = new ErrorVo();
        error.setCode(responseCode.code);
        return error;
    }
}
