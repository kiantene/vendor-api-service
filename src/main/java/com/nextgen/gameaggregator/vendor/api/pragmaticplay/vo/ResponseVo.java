package com.nextgen.gameaggregator.vendor.api.pragmaticplay.vo;

import lombok.Data;

@Data
public class ResponseVo {
    private Integer error;      // Response status
    private String description; // Response status short description
}
