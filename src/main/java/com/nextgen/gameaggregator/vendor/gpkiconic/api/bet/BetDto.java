package com.nextgen.gameaggregator.vendor.gpkiconic.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkiconic.constant.BetType;
import com.nextgen.gameaggregator.vendor.gpkiconic.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.gpkiconic.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends ActionDto implements BetResultData {


    @JsonIgnore
    private boolean settledByBet = false;

    @NotBlank
    @JsonProperty("api_token")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String apiToken;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("user")
    private String user;

    @NotNull
    @PositiveOrZero
    @JsonProperty("money")
    private BigDecimal money;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "\\d+")
    @JsonProperty("timestamp")
    private String timestamp;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    @JsonProperty("dealid")
    private String dealid;

    @NotBlank
    @Pattern(regexp = "[12]")
    @JsonProperty("code")
    private String code;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    @JsonProperty("roundid")
    private String bRoundid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("gameinfo")
    private String gameInfo;

    @NotBlank
    @Pattern(regexp = "\\d+")
    @JsonProperty("provider")
    private String provider;

    @Pattern(regexp = "true|false|1|0")
    @JsonProperty("istips")
    private String isTips;


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
        return this.bRoundid;
    }

    @Override
    public String getGameId() {
        return this.gameInfo;
    }

    @Override
    public BigDecimal getBetAmount() {
        if (this.getCode().equals(BetType.POINTIN)) {
            return this.getMoney().abs();
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.getCode().equals(BetType.POINTOUT)) {
            return this.getMoney();
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
        BetStatus status;
        //normal bet
        if (this.getCode().equals(BetType.POINTIN)) {
            // unsettled
            status = BetStatus.UNSETTLED;
        } else {
            // settled
            status = BetStatus.SETTLED;
        }
        return status;
    }

    @Override
    public boolean getShouldSettleByBet() {
        return this.settledByBet;
    }
}
