package com.nextgen.gameaggregator.vendor.ag.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Data")
public class CommonDto implements BetResultData {

    private static final String WITHDRAW = "WITHDRAW";
    private static final String DEPOSIT = "DEPOSIT";

    @JacksonXmlProperty(localName = "Record")
    private RecordDto recordDto;

    @Override
    public String getExternalTransactionId() {
        return this.recordDto.getTransactionID();
    }

    @Override
    public String getVendorBetId() {
        return this.recordDto.getTransactionID();
    }
    
    @Override
    public String getRoundId() {
        return this.recordDto.getRoundId();
    }

    @Override
    public String getGameId() {
        return this.recordDto.getGameId();
    }

    @Override
    public BigDecimal getBetAmount() {
        if (WITHDRAW.equals(recordDto.getTransactionType())) {
            return this.recordDto.getAmount();
        } else if (DEPOSIT.equals(recordDto.getTransactionType())) {
            return null;
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (WITHDRAW.equals(recordDto.getTransactionType())) {
            return null;
        } else if (DEPOSIT.equals(recordDto.getTransactionType())) {
            return this.recordDto.getAmount();
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
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        if (WITHDRAW.equals(recordDto.getTransactionType())) {
            return BetStatus.UNSETTLED;
        } else if (DEPOSIT.equals(recordDto.getTransactionType())) {
            return BetStatus.SETTLED;
        }
        return null;
    }
}
