package com.nextgen.gameaggregator.vendor.aviatrix.api.kiv_closematch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.aviatrix.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloseMatchDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    public String cid;
    @NotBlank
    @Size(max = 50)
    public String playerId;
    @NotBlank
    @Size(max = 255)
    public String productId;
    @NotBlank
    @Size(max = 255)
    public String matchId; //round id
    @NotBlank
    @Size(max = 255)
    public String txId;

    @Override
    public String getExternalTransactionId() {
        return this.txId;
    }

    @Override
    public String getVendorBetId() {
        return this.txId;
    }

    @Override
    public String getRoundId() {
        return this.matchId;
    }

    @Override
    public String getGameId() {
        return this.productId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
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
        return null;
    }

    @Override
    public Long getResultTime() {
        return this.getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return this.getTimestamp();
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

    public Long getTimestamp() {
        Instant instant = Instant.parse(VendorService.returnTime());
        return instant.toEpochMilli();
    }
}
