package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.alize.constant.ResponseCode;

import lombok.Data;

@Data
public class CommonVo implements HttpResponse {
    private ResultVo Result;
    private Integer ErrorCode;

    public String getTimestamp() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }

    public String setTimestamp(String timestamp) {
        return timestamp;
    }

    @Override
    public boolean hasError() {
        return !this.ErrorCode.equals(ResponseCode.SUCCESS.code);
    }
}
