package com.nextgen.gameaggregator.vendor.avatarux.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorVo implements HttpResponse {
    private String message;

    private String code;

    @Override
    public boolean hasError() {
        return false;
    }
}
