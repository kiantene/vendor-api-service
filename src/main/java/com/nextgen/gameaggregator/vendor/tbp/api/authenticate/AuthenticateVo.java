package com.nextgen.gameaggregator.vendor.tbp.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo implements HttpResponse {

    @JsonProperty("Username")
    private String username;

    @JsonProperty("Password")
    private String password;

    @JsonProperty("SessionId")
    private String sessionId;

    @JsonProperty("UserId")
    private String userId;

    @JsonProperty("Currency")
    private String currency;

    @JsonProperty("Balance")
    private BigDecimal balance;

    @JsonProperty("Successful")
    private Boolean successful;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("ErrorCode")
    private int errorCode;

    @Override
    public boolean hasError() {
        return true;
    }
}