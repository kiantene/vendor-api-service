package com.nextgen.gameaggregator.vendor.avatarux.api.betnsettle;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class BetNSettleDto implements BetResultData {

    @NotBlank
    private String authorization;

    @NotBlank
    private String xServerAuthorization;

    @NotBlank
    @Size(max = 255)
    private String nativeId;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    private String type;

    @NotBlank
    @Size(max = 255)
    private String provider;

    @Digits(integer = 20, fraction = 8)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    private String campaignType;

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
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return "";
    }

    @Override
    public BigDecimal getBetAmount() {
        if (this.type.equals("withdraw")) {
            return this.amount;
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.type.equals("deposit")) {
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
        if ("freeBets".equals(this.campaignType)) {
            return 1;
        }
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        if (this.type.equals("deposit")) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}