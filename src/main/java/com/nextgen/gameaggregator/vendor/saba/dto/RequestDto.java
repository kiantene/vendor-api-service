package com.nextgen.gameaggregator.vendor.saba.dto;

import lombok.Data;

@Data
public class RequestDto<T> {
    private String key;
    private T message;
}
