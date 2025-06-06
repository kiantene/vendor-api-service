package com.nextgen.gameaggregator.vendor.wmlive.api.betandsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.wmlive.api.action.GeneralActionDto;
import com.nextgen.gameaggregator.vendor.wmlive.constant.BetType;
import com.nextgen.gameaggregator.vendor.wmlive.constant.Formats;
import com.nextgen.gameaggregator.vendor.wmlive.service.VendorService;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointInOutDto extends GeneralActionDto implements BetResultData {

    @Override
    public String getExternalTransactionId() {
        return this.getDealid();
    }

    @Override
    public String getVendorBetId() {
        return this.getDealid();
    }

    @Override
    public String getRoundId() {
        return this.getGameno();
    }

    @Override
    public String getGameId() {
        return this.getGtype();
    }

    @Override
    public BigDecimal getBetAmount() {
        if (this.getCode().equals(BetType.POINTOUT)) {
            return this.getMoney().abs();
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.getCode().equals(BetType.POINTIN)) {
            return this.getMoney().abs();
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
        return VendorService.convertDateTimeStringToTimestamp(this.getRequestDate(), Formats.DATE_TIME_FORMAT);
    }

    @Override
    public Long getResultTime() {
        Long date = null;
        if (this.getCode().equals(BetType.POINTIN) || this.getCategory().equals(BetType.TIPS)) {
            date = VendorService.convertDateTimeStringToTimestamp(this.getRequestDate(), Formats.DATE_TIME_FORMAT);
        }
        return date;
    }

    @Override
    public Long getVendorSettleTime() {
        Long date = null;
        if (this.getCode().equals(BetType.POINTIN) || this.getCategory().equals(BetType.TIPS)) {
            date = VendorService.convertDateTimeStringToTimestamp(this.getRequestDate(), Formats.DATE_TIME_FORMAT);
        }
        return date;
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
        if (this.getCode().equals(BetType.POINTIN) || this.getCategory().equals(BetType.TIPS)) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}
