package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.BetType;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.PlatformType;
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

    @Pattern(regexp = "[01]")
    private String finished;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameinfo;

    @PositiveOrZero
    private Double betinfo;

    @NotBlank
    @Pattern(regexp = "\\d+")
    private String platform;

    @Pattern(regexp = "[01]")
    private String istips;

    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String root_roundid;

    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String root_dealid;

    @Override
    public String getExternalTransactionId() {
        String exTransId = this.dealid;

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            // dealid equals to null mean did not get any win amount in buy bonus game
            if(this.dealid == null){
                exTransId = this.roundid;
            }
        }

        return exTransId;
    }

    @Override
    public String getVendorBetId() {
        String vendorBetId = this.dealid;

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            // dealid equals to null mean did not get any win amount in buy bonus game
            if(this.dealid == null){
                vendorBetId = this.roundid;
            }
        }

        return vendorBetId;
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
        BigDecimal betAmount = null;

        // if not booming platform then mean normal bet amount
        if(!this.platform.equals(PlatformType.BOOMING) || !this.platform.equals(PlatformType.BOOMINGLATAM)){
            if(this.code.equals(BetType.POINTIN)){
                betAmount = BigDecimal.valueOf(this.money);
            }
        }else{
            betAmount = BigDecimal.valueOf(this.betinfo);
        }

        return betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal winAmount = null;

        // if not booming platform then mean normal bet amount
        if(!this.platform.equals(PlatformType.BOOMING) || !this.platform.equals(PlatformType.BOOMINGLATAM)) {
            if (this.code.equals(BetType.POINTOUT)) {
                winAmount = BigDecimal.valueOf(this.money);
            }
        }

        return winAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        BigDecimal winLoss = null;

        // booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)) {
            winLoss = new BigDecimal(this.getCode().equals(BetType.POINTIN) ? (this.getMoney() * -1.00) : this.getMoney());
        }

        return winLoss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        BigDecimal turnover = null;

        // if not booming platform then mean normal bet amount
        if(!this.platform.equals(PlatformType.BOOMING) || !this.platform.equals(PlatformType.BOOMINGLATAM)) {
            if(this.code.equals(BetType.POINTIN)){
                turnover = BigDecimal.valueOf(this.money);
            }
        }else{
            turnover = BigDecimal.valueOf(this.betinfo);
        }

        return turnover;
    }

    @Override
    public Long getVendorBetTime() {
        Long betTime = null;

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            if(this.istips.equals(BetType.TIPS)){
                // tips
                betTime = Long.parseLong(this.timestamp) * 1000;
            }else{
                // place bet
                if(this.finished.equals(BetType.UNFINISHED)){
                    betTime = Long.parseLong(this.timestamp) * 1000;
                }
            }
        }

        //turbo game
        if(this.platform.equals(PlatformType.TURBOGAME) || this.platform.equals(PlatformType.TURBOGAMELATAM)){
            if(this.dealid.contains("place") && this.finished == null && this.code.equals(BetType.POINTIN)){
                // place bet
                betTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            if((this.finished.equals(BetType.UNFINISHED) && this.code.equals(BetType.POINTIN)) || (this.finished.equals(BetType.FINISHED) && this.code.equals(BetType.POINTIN))){
                // place bet or straightly lose
                betTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            // only one-time settledment
            betTime = Long.parseLong(this.timestamp) * 1000;
        }

        return betTime;
    }

    @Override
    public Long getResultTime() {
        return VendorService.getMilSec();
    }

    @Override
    public Long getVendorSettleTime() {
        Long settledTime = null;

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            if(this.istips.equals(BetType.TIPS)){
                // tips
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }else{
                // settled
                if(this.finished.equals(BetType.FINISHED)){
                    settledTime = Long.parseLong(this.timestamp) * 1000;
                }
            }
        }

        //turbo game
        if(this.platform.equals(PlatformType.TURBOGAME) || this.platform.equals(PlatformType.TURBOGAMELATAM)){
            if(this.dealid.contains("settle") && this.finished.equals(BetType.FINISHED)){
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            if(this.finished.equals(BetType.FINISHED)){
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            // only one-time settlement
            settledTime = Long.parseLong(this.timestamp) * 1000;
        }

        return settledTime;
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
        BetStatus status = null;

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            if(this.istips.equals(BetType.TIPS)){
                // tips
                status = BetStatus.SETTLED;
            }else{
                //normal bet
                if(this.finished.equals(BetType.UNFINISHED)){
                    // unsettled
                    status = BetStatus.UNSETTLED;
                }else{
                    // settled
                    status = BetStatus.SETTLED;
                }
            }
        }

        //turbo game
        if(this.platform.equals(PlatformType.TURBOGAME) || this.platform.equals(PlatformType.TURBOGAMELATAM)){
            if(this.dealid.contains("place") && this.finished == null){
                status = BetStatus.UNSETTLED;
            }else{
                status =  BetStatus.SETTLED;
            }
        }

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            if(this.finished.equals(BetType.UNFINISHED)){
                status = BetStatus.UNSETTLED;
            }else{
                status = BetStatus.SETTLED;
            }
        }

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            if(this.finished.equals(BetType.UNFINISHED)){
                status = BetStatus.UNSETTLED;
            }else{
                status = BetStatus.SETTLED;
            }
        }

        return status;
    }
}
