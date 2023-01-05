package com.nextgen.gameaggregator.vendor.cq9.vo;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public class StatusVo {
    // This variable will be 0 when there is no error.
    @Nullable
    private String code = "0";
    private String message = "Success"; // Success
    private String dateTime; // DateTime format = RFC3339
}
