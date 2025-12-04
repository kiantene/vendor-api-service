package com.nextgen.gameaggregator.vendor.ezugi.api.v2.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRollbackRequest {
    private Integer operatorId;
    @NotBlank
    private String uid;
    @NotBlank
    private String transactionId;
    @NotNull
    private String roundId;
    @NotNull
    private Integer gameId;
    @NotBlank
    private String currency;
    private BigDecimal rollbackAmount;
    @NotBlank
    private String token;
    @NotNull
    private Long timestamp;
}
