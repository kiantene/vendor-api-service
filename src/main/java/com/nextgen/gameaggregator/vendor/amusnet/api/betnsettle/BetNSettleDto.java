package com.nextgen.gameaggregator.vendor.amusnet.api.betnsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.amusnet.dto.CommonDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JacksonXmlRootElement(localName = "WithdrawAndDepositRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto extends CommonDto implements BetResultData {

    @JacksonXmlProperty(localName = "WinAmount")
    @Digits(integer = 20, fraction = 8)
    @NotNull
    private BigDecimal vendorWinAmount;

    @Override
    public String getExternalTransactionId() {
        return this.getTransferId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransferId();
    }

    @Override
    public String getRoundId() {
        return this.getGameNumber();
    }

    @Override
    public String getGameId() {
        return this.getVendorGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getAmount();
    }

    @Override
    public BigDecimal getWinAmount() {
        return getVendorWinAmount();
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
        return System.currentTimeMillis();
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
