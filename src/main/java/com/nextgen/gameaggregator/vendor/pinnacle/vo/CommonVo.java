package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("TransactionId")
    private Long transactionId;
    @JsonProperty("WagerId")
    private Long wagerId;
    @JsonProperty("ResponseCode")
    private Integer responseCode = ResponseCode.SUCCESS.code;
    @JsonIgnore
    private BigDecimal balance;
    @JsonIgnore
    private Boolean setResponseVoErrorCode = Boolean.FALSE;

    public CommonVo(Long id, Long transactionId, Long wagerId) {
        this.id = id;
        this.transactionId = transactionId;
        this.wagerId = wagerId;
    }
}
