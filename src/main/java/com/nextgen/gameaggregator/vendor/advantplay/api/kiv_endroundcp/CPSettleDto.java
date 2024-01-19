package com.nextgen.gameaggregator.vendor.advantplay.api.kiv_endroundcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.advantplay.dto.BetSettleRefundDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class CPSettleDto extends BetSettleRefundDto implements BetResultData {

    @JsonProperty("CPId")
    private String cpId;

    @JsonProperty("CPType")
    private String cpType;

    @JsonProperty("CPTransId")
    private String cpTransId;

    @JsonProperty("CPWin")
    private BigDecimal cpWin;

    @Override
    public String getExternalTransactionId() {
        return this.getGameRoundId();
    }

    @Override
    public String getVendorBetId() {
        return this.getGameRoundId();
    }

    @Override
    public String getRoundId() {
        return this.getGameRoundId();
    }

    @Override
    public String getGameId() {
        return this.getGameCode();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getCpWin();
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
        return null;
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
        return BetStatus.SETTLED;
    }
}
