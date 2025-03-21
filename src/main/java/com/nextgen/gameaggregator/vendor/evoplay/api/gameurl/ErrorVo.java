package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import lombok.Getter;

import java.util.List;

@Getter
public class ErrorVo {
    private Integer code;
    private String scope;
    private String message;
    private List<?> info;

}
