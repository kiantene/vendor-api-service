package com.nextgen.gameaggregator.vendor.cq9.dto;

import lombok.Data;

@Data
public class ResponseDto<T> {
    private T data;
    private StatusDto status;
}
