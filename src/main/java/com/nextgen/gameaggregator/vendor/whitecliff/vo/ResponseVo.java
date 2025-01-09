package com.nextgen.gameaggregator.vendor.whitecliff.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseCodes;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    @NotNull
    private Integer status = 0;

    private BigDecimal balance;

    private String error;

    @Override
    public boolean hasError() {
        return !status.equals(ResponseCodes.SUCCESS);
    }
}
