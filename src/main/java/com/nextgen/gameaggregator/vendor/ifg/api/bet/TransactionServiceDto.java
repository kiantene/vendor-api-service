package com.nextgen.gameaggregator.vendor.ifg.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JacksonXmlRootElement(localName = "server")
public class TransactionServiceDto implements BetResultData {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String session;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}")
    private String time;

    @JacksonXmlProperty(localName = "roundbet")
    @NotNull
    private RoundBetDto roundbet;

    @Override
    public String getExternalTransactionId() {
        return this.getRoundbet().getId();
    }

    @Override
    public String getVendorBetId() {
        return this.getRoundbet().getId();
    }

    @Override
    public String getRoundId() {
        return this.getRoundbet().getRoundnum().getId();
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return new BigDecimal(this.getRoundbet().getBet());
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimeStamp(this.getTime());
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
        return BetStatus.UNSETTLED;
    }

}
