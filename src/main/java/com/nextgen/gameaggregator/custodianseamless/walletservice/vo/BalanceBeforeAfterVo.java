package com.nextgen.gameaggregator.custodianseamless.walletservice.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.operator.vo.ResponseVo;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceBeforeAfterVo extends ResponseVo {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ResponseData {
        private String transactionId;
        private String referenceId;

        private String username;
        private Integer tokenId;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private Long completedAt;
    }

    @Valid
    private ResponseData data;
}

