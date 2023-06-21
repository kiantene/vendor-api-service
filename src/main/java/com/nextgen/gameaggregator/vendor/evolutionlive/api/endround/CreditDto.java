package com.nextgen.gameaggregator.vendor.evolutionlive.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.DebitCreditCancelDto;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto extends DebitCreditCancelDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return this.getTransaction().getRefId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransaction().getRefId();
    } // id or refId

    @Override
    public String getRoundId() {
        // Vendor BackOffice only use front ID
        // e.g. (1766426e099ddd0a3aa82cba-rcj5y4fzmrmqaqtj) only ID before "-" needed
        return this.getGame().getId().split("-")[0];
    }

    @Override
    public String getGameId() {
        return this.getGame().getDetails().getTable().getId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getTransaction().getAmount();
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return getBetAmount();
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
