package com.nextgen.gameaggregator.vendor.pragmaticplay.vo;

import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCodes;
import lombok.Data;

@Data
public class ResponseVo {
    private Integer error;      // Response status
    private String description; // Response status short description

    public ResponseVo() {
        this.error = ResponseCodes.SUCCESS;
    }
}
