package com.nextgen.gameaggregator.vendor.bglive.api.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryVo {
    private String orderId;
    private int status;

    public QueryVo(String orderId, Integer status) {
        this.orderId = orderId;
        this.status = status;
    }
}
