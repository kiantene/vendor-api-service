package com.nextgen.gameaggregator.vendor.tbp.api.cancel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import lombok.Setter;

import java.math.BigDecimal;


@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelVo implements HttpResponse {

    @JsonProperty("Balance")
    private BigDecimal balance;

    @JsonProperty("CasinoTransferId")
    private String casinoTransferId;

    @JsonProperty("ErrorCode")
    private int errorCode;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("Successful")
    private Boolean successful;

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