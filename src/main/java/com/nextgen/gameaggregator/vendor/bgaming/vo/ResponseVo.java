package com.nextgen.gameaggregator.vendor.bgaming.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.bgaming.constant.ResponseCodes;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    @JsonProperty("balance")
    private Integer balance;
    @JsonProperty("game_id")
    private String gameId;
    @JsonProperty("transactions")
    private List<TransactionVo> transactions;
    @JsonProperty("code")
    private Integer code;
    @JsonProperty("message")
    private String message;
    @JsonIgnore
    private Integer httpStatus;
    @JsonIgnore
    private ResponseCodes responseCodes;

    public ResponseVo() {
        this.setResponseCodes(ResponseCodes.SUCCESS);
    }

    public void addTransactions(TransactionVo transactionVo) {
        if (this.getTransactions() == null) {
            this.setTransactions(new LinkedList<>());
        }
        this.getTransactions().add(transactionVo);
    }

    public void setResponseCodes(ResponseCodes responseCodes) {
        this.responseCodes = responseCodes;
        this.code = responseCodes.code;
        this.message = responseCodes.message;
        this.httpStatus = responseCodes.httpStatus;
    }

    @Override
    public boolean hasError() {
        return !this.responseCodes.equals(ResponseCodes.SUCCESS);
    }
}