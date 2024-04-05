package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    @JsonProperty("Result")
    private ResultVo result;
    @JsonProperty("ErrorCode")
    private Integer errorCode = ResponseCode.SUCCESS.code;

    @JsonProperty("Timestamp")
    public String getTimestamp() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return currentDateTime.format(formatter);
    }

    @Override
    public boolean hasError() {
        return !this.errorCode.equals(ResponseCode.SUCCESS.code);
    }
}