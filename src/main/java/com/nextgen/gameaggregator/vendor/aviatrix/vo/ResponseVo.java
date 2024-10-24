package com.nextgen.gameaggregator.vendor.aviatrix.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    private String createdAt;
    private BigInteger balance;
    private String message;

    @JsonIgnore
    private HttpStatus httpStatus = HttpStatus.OK;

    @Override
    public boolean hasError() {
        return !httpStatus.equals(HttpStatus.OK);
    }
}
