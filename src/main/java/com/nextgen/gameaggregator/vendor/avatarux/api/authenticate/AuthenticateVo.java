package com.nextgen.gameaggregator.vendor.avatarux.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.avatarux.vo.ErrorVo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo implements HttpResponse {
    @JsonProperty("nativeId")
    private String nativeId;

    @JsonProperty("token")
    private String token;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("error")
    private ErrorVo error;

    @Override
    public boolean hasError() {
        return this.error != null;
    }
}