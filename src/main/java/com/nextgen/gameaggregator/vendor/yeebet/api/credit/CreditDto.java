package com.nextgen.gameaggregator.vendor.yeebet.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.yeebet.dto.BetsDto;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreditDto implements BetResultData {

    @NotBlank
    private String appid;

    @NotBlank
    private String username;

    @NotBlank
    private String amount;

    @NotBlank
    private String notifyid;

    @NotBlank
    // must be either  7 or 9
    @Pattern(regexp = "[79]")
    private String type;

    @NotBlank
    private String serialnumber;

    @NotBlank
    private String sign;

    @NotBlank
    private String bets;

    @NotNull
    private BetsDto betsDto;

    public void convertBetToDto() throws JsonProcessingException {
        if(this.bets != null) {
            this.betsDto = new ObjectMapper().readValue(this.bets.toString(), BetsDto.class);
        }
    }

    @Override
    public String getExternalTransactionId() {
        return this.getSerialnumber();
    }

    @Override
    public String getVendorBetId() {
        return this.getBetsDto().getId();
    }

    @Override
    public String getRoundId() {
        return this.getBetsDto().getId();
    }

    @Override
    public String getGameId() {
        return this.getBetsDto().getGameid();
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal betAmount = new BigDecimal(this.getBetsDto().getBetamount());

        BigDecimal winLoss = new BigDecimal(this.getBetsDto().getWinlost());

        return betAmount.add(winLoss);
    }

    @Override
    public BigDecimal getWinLoss() {
        return new BigDecimal(this.getBetsDto().getWinlost());
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
        return Instant.now().toEpochMilli();
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.getTimeStamp(this.getBetsDto().getSettletime());
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
