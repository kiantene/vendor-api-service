package com.nextgen.gameaggregator.vendor.spinix.dto;

import lombok.Data;

@Data
public class ResponseDto<T> {
    private T data;
    private StatusDto status;
}
