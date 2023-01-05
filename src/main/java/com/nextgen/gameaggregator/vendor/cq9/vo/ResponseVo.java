package com.nextgen.gameaggregator.vendor.cq9.vo;

import lombok.Data;

@Data
public class ResponseVo<T> {
    private T data;
    private StatusVo status;
}
