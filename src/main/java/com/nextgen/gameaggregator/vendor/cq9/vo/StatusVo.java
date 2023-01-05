package com.nextgen.gameaggregator.vendor.cq9.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class StatusVo {
    private String code = "0"; // This variable will be 0 when there is no error.
    private String message = "Success"; // Success
    private String dateTime; // DateTime format = RFC3339

    @JsonIgnore
    public boolean isError() {
        return !this.code.equals("0");
    }
}
