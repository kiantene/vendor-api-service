package com.nextgen.gameaggregator.vendor.aasexy.api.tips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.aasexy.dto.TipsInfoDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipsTransactionsDto implements BetResultData {
    @NotBlank
    @Size(min = 1, max = 255)
    private String platformTxId;

    @NotBlank
    @Size(max=50)
    private String userId;

    @NotBlank
    private String currency;

    private String platform;

    private String gameType;

    @NotBlank
    @Size(max = 255)
    private String gameCode;

    private String gameName;

    private String type;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal tip;

    private String txTime;

    private TipsInfoDto tipInfo;

    @Override
    public String getExternalTransactionId() {
        return this.platformTxId;
    }

    @Override
    public String getVendorBetId() {
        return this.platformTxId;
    }

    @Override
    public String getRoundId() {
        return this.platformTxId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.tip;
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
        return VendorService.getTimeStamp(this.txTime);
    }

    @Override
    public Long getResultTime() {
        return VendorService.getTimeStamp(this.txTime);
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getTimeStamp(this.txTime);
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
