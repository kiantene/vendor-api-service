package com.nextgen.gameaggregator.vendor.aasexy.dto;

import lombok.Data;

@Data
public class RequestDto<T> {
    private String extension1;
    private String key;
    private T message;
}
