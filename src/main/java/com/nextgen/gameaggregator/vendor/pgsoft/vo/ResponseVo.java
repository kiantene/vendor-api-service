package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import lombok.Data;

@Data
public class ResponseVo<T> extends CommonVo {
    private T data;
}
