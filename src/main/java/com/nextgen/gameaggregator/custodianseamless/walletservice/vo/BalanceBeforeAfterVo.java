package com.nextgen.gameaggregator.custodianseamless.walletservice.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceBeforeAfterVo extends ResponseVo {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ResponseData {
        private List<String> transactionId;
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

