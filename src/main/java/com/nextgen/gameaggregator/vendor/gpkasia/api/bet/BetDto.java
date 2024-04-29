package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends ActionDto implements BetResultData {
    @Autowired
    VendorService vendorService;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String api_token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user;

    @NotNull
    @PositiveOrZero
    private Double money;

    @NotBlank
    @Size(min = 10, max = 10)
    @Pattern(regexp = "\\d+")
    private String timestamp;

    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String dealid;

    @NotBlank
    @Pattern(regexp = "[12]")
    private String code;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String roundid;

//    @Pattern(regexp = "[01]")
//    private String finished;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameinfo;

    @NotBlank
    @Pattern(regexp = "\\d+")
    private String platform;

    @Pattern(regexp = "[01]")
    private String istips;

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
            return BigDecimal.valueOf(this.money);
        }

        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        // 1 is increase, 2 is decrease
        if(this.code.equals("1")){
            return BigDecimal.valueOf(this.money);
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
            return BigDecimal.valueOf(this.money);
        }

        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return Long.parseLong(this.timestamp) * 1000;
    }

    @Override
    public Long getResultTime() {
        return VendorService.getMilSec();
    }

    @Override
    public Long getVendorSettleTime() {
        return Long.parseLong(this.timestamp) * 1000;
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
