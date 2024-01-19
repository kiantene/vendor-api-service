package com.nextgen.gameaggregator.vendor.yeebet.api.debit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.dto.BetsDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto implements BetResultData {

    @NotBlank
    private String appid;

    @NotBlank
    private String username;

    @NotBlank
    private String amount;

    @NotBlank
    private String notifyid;

    @NotBlank
    // must be either  1 or 7
    @Pattern(regexp = "[17]")
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
        if(this.bets != null){
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
        return new BigDecimal(this.getBetsDto().getBetamount());
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
        return new BigDecimal(this.getBetsDto().getBetamount());
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimeStamp(this.getBetsDto().getCreatetime());
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
        return BetStatus.UNSETTLED;
    }
}
