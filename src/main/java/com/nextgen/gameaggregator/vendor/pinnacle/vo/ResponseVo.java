package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.alize.constant.ResponseCode;

import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {
    @JsonProperty("Result")
    private ResultVo result;

    @JsonProperty("ErrorCode")
    private Integer errorCode;

    @JsonProperty("Timestamp")
    public String getTimestamp() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }

    @Override
    public boolean hasError() {
        return !this.errorCode.equals(ResponseCode.SUCCESS.code);
    }
}