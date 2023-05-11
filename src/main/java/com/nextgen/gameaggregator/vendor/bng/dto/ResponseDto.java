package com.nextgen.gameaggregator.vendor.bng.dto;

import lombok.Data;

@Data
public class ResponseDto<T> {
    private T data;
    private StatusDto status;
}
