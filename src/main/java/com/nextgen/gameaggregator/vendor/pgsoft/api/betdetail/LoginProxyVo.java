package com.nextgen.gameaggregator.vendor.pgsoft.api.betdetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class LoginProxyVo {

    @Valid
    private ResponseData data;

    private String error;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ResponseData {
        @NotBlank(message = "operator_session can not be blank")
        private String operator_session;
    }

}
