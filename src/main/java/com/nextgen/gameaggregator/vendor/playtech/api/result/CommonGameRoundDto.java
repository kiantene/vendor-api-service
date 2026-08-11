package com.nextgen.gameaggregator.vendor.playtech.api.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playtech.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playtech.dto.GameRoundCloseDto;
import com.nextgen.gameaggregator.vendor.playtech.dto.LiveTableDetailsDto;
import com.nextgen.gameaggregator.vendor.playtech.dto.PayDto;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonGameRoundDto extends CommonDto implements BetResultData, RollbackData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameRoundCode")
    private String gameRoundCode;

    @JsonProperty("pay")
    private PayDto pay;

    @JsonProperty("gameRoundClose")
    private GameRoundCloseDto gameRoundClose;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameCodeName")
    private String gameCodeName;

    @JsonIgnore
    private Long timeStamp = System.currentTimeMillis();

    @JsonProperty("liveTableDetails")
    private LiveTableDetailsDto liveTableDetails;

    @Override
    public String getExternalTransactionId() {
        if (this.pay != null && this.pay.getTransactionCode() != null) {
            return this.pay.getTransactionCode();
        } else
            return this.gameRoundCode;
    }

    @Override
    public String getVendorBetId() {
        if (this.pay != null && this.pay.getTransactionCode() != null) {
            return this.pay.getTransactionCode();
        } else
            return this.gameRoundCode;
    }

    @Override
    public String getRollbackId() {
        return this.pay.getRelatedTransactionCode();
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return this.gameRoundCode;
    }

    @Override
    public String getGameId() {
        return this.gameCodeName;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (pay != null && pay.getAmount() != null) {
            return pay.getAmount();
        }
        return BigDecimal.ZERO;

    }

    @Override
    public BigDecimal getWinLoss() {
        return this.getWinAmount();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return this.getVendorSettleTime();
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        if (pay != null && pay.getTransactionDate() != null) {
            return VendorService.convertStringToMillis(pay.getTransactionDate());
        } else if (gameRoundClose != null && gameRoundClose.getDate() != null) {
            return VendorService.convertStringToMillis(gameRoundClose.getDate());
        }
        return timeStamp;
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
        return this.gameRoundClose == null ? BetStatus.UNSETTLED : BetStatus.SETTLED;
    }


}

