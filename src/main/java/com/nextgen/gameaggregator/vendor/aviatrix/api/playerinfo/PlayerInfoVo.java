package com.nextgen.gameaggregator.vendor.aviatrix.api.playerinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerInfoVo implements HttpResponse {

    private String playerId;
    private BigInteger balance;
    private String currency;
    private String message;

    @JsonIgnore
    private HttpStatus httpStatus = HttpStatus.OK;

    @Override
    public boolean hasError() {
        return !httpStatus.equals(HttpStatus.OK);
    }
}
