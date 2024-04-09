package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.dto.ActionDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends ActionDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String api_token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user;

    private Double money;

    private String timestamp;

    private String dealid;

    private String code;

    private String roundid;

    private String finished;

    private String gameinfo;

    private String platform;

    @Override
    public String getExternalTransactionId() {
        return this.dealid;
    }

    @Override
    public String getVendorBetId() {
        return this.dealid;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gameinfo;
    }

    @Override
    public BigDecimal getBetAmount() {
        // 1 is increase, 2 is decrease
        if(this.code.equals("2")){
            return new BigDecimal(this.money);
        }

        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        // 1 is increase, 2 is decrease
        if(this.code.equals("1")){
            return new BigDecimal(this.money);
        }

        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        // 1 is increase, 2 is decrease
        if(this.code.equals("2")){
            return new BigDecimal(this.money);
        }

        return null;
    }

    @Override
    public Long getVendorBetTime() {
        // 1 is increase, 2 is decrease
        if(this.code.equals("2")){
            return Long.parseLong(this.timestamp) * 1000;
        }

        return null;
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
        // 1 is increase, 2 is decrease
        if(this.code.equals("2")){
            return BetStatus.UNSETTLED;
        }

        return BetStatus.SETTLED;
    }
}
