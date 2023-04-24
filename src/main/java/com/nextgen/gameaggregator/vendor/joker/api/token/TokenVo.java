package com.nextgen.gameaggregator.vendor.joker.api.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenVo implements HttpResponse {

    @JsonProperty("Username")
    private String username;
    @JsonProperty("Balance")
    private Double balance;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Integer status;

    public TokenVo() {
        this.setResponseCode(ResponseCodes.SUCCESS);
    }

    public void setResponseCode(String responseCode) {
        this.status = Integer.valueOf(responseCode);
        this.message = ResponseCodes.RESPONSE_DESCRIPTION.get(responseCode);
        this.username = "";
        this.balance = Double.valueOf(0);
    }
    @Override
    public boolean hasError() {
        return false;
    }
}
