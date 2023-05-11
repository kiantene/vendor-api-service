package com.nextgen.gameaggregator.vendor.bng.vo;

import lombok.Data;

@Data
public class StatusVo {
    private String code = "0"; // This variable will be 0 when there is no error.
    private String message; // Success
    private String dateTime; // DateTime format = ISO8601
}
