package com.nextgen.gameaggregator.vendor.smartsoft.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements BetResultData {

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

    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Amount")
    private BigDecimal amount;

    @NotBlank
    @JsonProperty("TransactionType")
    private String transactionType;

    @NotBlank
    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @JsonProperty("TransactionInfo")
    private SettleTransactionInfoDto settleTransactionInfoDto;

    @Override
    public String getExternalTransactionId() {
        return this.transactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.transactionId;
    }

    @Override
    public String getRoundId() {
        return this.settleTransactionInfoDto.getRoundId();
    }

    @Override
    public String getGameId() {
        return "";
    }

    @Override
    public BigDecimal getBetAmount() {
        if (this.transactionType.equals("InitialBet") || this.transactionType.equals("PlaceBet")) {
            return this.amount;
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.transactionType.equals("WinAmount") || this.transactionType.equals("CloseRound")) {
            return this.amount;
        }
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
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
        return null;
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
        if (this.transactionType.equals("WinAmount")) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}