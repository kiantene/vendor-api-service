package com.nextgen.gameaggregator.vendor.ifg.api.endround;

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
public class CreditServiceDto implements BetResultData {
    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String session;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}")
    private String time;

    @JacksonXmlProperty(localName = "roundwin")
    @NotNull
    private RoundWinDto roundWinDto;

    @Override
    public String getExternalTransactionId() {
        return this.getRoundWinDto().getId();
    }

    @Override
    public String getVendorBetId() {
        return this.getRoundWinDto().getId();
    }

    @Override
    public String getRoundId() {
        return this.getRoundWinDto().getRoundNumDto().getId();
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return new BigDecimal(Integer.valueOf(this.getRoundWinDto().getWin()));
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
        return VendorService.getTimeStamp(this.getTime());
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getTimeStamp(this.getTime());
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        if(this.getRoundWinDto().getFinished().equals("0")){
            return 1;
        }else{
            return 0;
        }
    }

    @Override
    public BetStatus getBetStatus() {
        if(this.getRoundWinDto().getFinished().equals("0")){
            return BetStatus.UNSETTLED;
        }else{
            return BetStatus.SETTLED;
        }
    }
}
