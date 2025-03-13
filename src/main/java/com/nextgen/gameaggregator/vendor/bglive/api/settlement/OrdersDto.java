package com.nextgen.gameaggregator.vendor.bglive.api.settlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdersDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("orderId")
    private String orderId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameId")
    private String gameId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JacksonXmlProperty(localName = "amount")
    private BigDecimal amount;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JacksonXmlProperty(localName = "orderAmount")
    private BigDecimal orderAmount;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JacksonXmlProperty(localName = "validAmount")
    private BigDecimal validAmount;


    @Override
    public String getExternalTransactionId() {
        return this.orderId;

    }

    @Override
    public String getVendorBetId() {
        return this.getExternalTransactionId();
    }

    @Override
    public String getRoundId() {
        return this.getExternalTransactionId();
    }

    @Override
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getValidAmount().abs();
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getAmount().abs();
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}