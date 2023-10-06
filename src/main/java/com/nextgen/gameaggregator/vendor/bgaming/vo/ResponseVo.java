package com.nextgen.gameaggregator.vendor.bgaming.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

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
    private Integer httpStatus;
    private List<HttpRequestLog> httpRequestLogList;

    @Override
    public boolean hasError() {
        return this.code == null ? false : true;
    }
}
