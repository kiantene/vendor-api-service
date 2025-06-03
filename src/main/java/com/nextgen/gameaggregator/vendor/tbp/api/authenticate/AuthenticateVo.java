package com.nextgen.gameaggregator.vendor.tbp.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import lombok.Setter;

import java.math.BigDecimal;

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

    public void setError(ResponseCode responseCode) {
        this.errorCode = responseCode.code;
        this.errorMessage = responseCode.description;
        this.successful = responseCode.code == 1000;
    }

}