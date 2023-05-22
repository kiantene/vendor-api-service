package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import lombok.Data;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto extends CommonDto implements BetResultData {
    private String uid;
    private String transactionId;
    private String debitTransactionId;
    @JsonProperty("roundId")
    private String vendorRoundId;
    private String gameId;
    private Double creditAmount;
    private String gameDataString;
    private GameDataStringDto gameDataStringDto;

    @Override
    public String getExternalTransactionId() {
        return this.transactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.debitTransactionId;
    }

    @Override
    @JsonIgnore
    public String getRoundId() {
        return this.debitTransactionId;
    }

    @Override
    public BigDecimal getBetAmount() { return BigDecimal.ZERO; }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(this.getGameDataStringDto().getWinAmount());
    }

    @Override
    public BigDecimal getWinLoss() {
        Double winLossAmount = this.getGameDataStringDto().getWinAmount() - this.getGameDataStringDto().getBetAmount();
        return BigDecimal.valueOf(winLossAmount);
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.valueOf(this.getGameDataStringDto().getBetAmount());
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return getTimestamp();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
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
