package com.nextgen.gameaggregator.vendor.smartsoft.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {

    @NotBlank
    @Size(max = 255)
    private String signature;

    @NotBlank
    @Size(max = 255)
    private String sessionId;

    @NotBlank
    @Size(max = 255)
    private String userName;

    @NotBlank
    @Size(max = 255)
    private String clientExternalKey;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("TransactionId")
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("CurrentTransactionId")
    private String currentTransactionId;

    @NotBlank
    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Amount")
    private BigDecimal amount;

    @Valid
    @NotNull
    @JsonProperty("TransactionInfo")
    private RollbackTransactionInfoDto rollbackTransactionInfoDto;

    @Override
    public String getRollbackId() {
        return this.transactionId;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return this.rollbackTransactionInfoDto.getRoundId();
    }
}
