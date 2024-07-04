package com.nextgen.gameaggregator.vendor.pinnacle.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CommonVo {
    
    private Long id;
    private Long transactionId;
    private Long wagerId;
    private Integer responseCode = ResponseCode.SUCCESS.code;
    @JsonIgnore
    private BigDecimal balance = BigDecimal.ZERO;

    public CommonVo(Long id, Long transactionId, Long wagerId) {
        this.id = id;
        this.transactionId = transactionId;
        this.wagerId = wagerId;
    }
}
