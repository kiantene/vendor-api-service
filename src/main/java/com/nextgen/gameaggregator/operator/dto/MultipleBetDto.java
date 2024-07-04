package com.nextgen.gameaggregator.operator.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MultipleBetDto {
    private String betId;
    @JsonIgnore
    private String vendorBetId;
    @JsonIgnore
    private String externalTransactionId;
    private BigDecimal betAmount;

    public MultipleBetDto() {
        this.betId = UUID.randomUUID().toString();
    }

    public MultipleBetDto(MultipleBetDto dto, BigDecimal conversionRate) {
        this.betId = dto.getBetId();
        this.vendorBetId = dto.getVendorBetId();
        this.externalTransactionId = dto.getExternalTransactionId();
        this.betAmount = dto.getBetAmount().multiply(conversionRate);
    }
}
