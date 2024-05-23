package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Arrays;
import java.util.List;

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
    @JsonProperty("roundid")
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String bRoundid;

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
                exTransId = this.bRoundid;
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
                vendorBetId = this.bRoundid;
            }
        }

        return vendorBetId;
    }

    @Override
    public String getRoundId() {
        String roundId = this.bRoundid;

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            //not same mean it is the middle part or end of the bonus game transaction
            if(!(this.dealid.equals(this.root_dealid) && !(this.bRoundid.equals(this.root_roundid)))){
                roundId = this.root_roundid;
            }
        }

        return roundId;
    }

    @Override
    public String getGameId() {
        return this.gameinfo;
    }

    @Override
    public BigDecimal getBetAmount() {
        BigDecimal betAmount = null;

        List<String> BoomingPlatform = Arrays.asList(PlatformType.BOOMING, PlatformType.BOOMINGLATAM);

        // not booming platform
        if(!BoomingPlatform.contains(this.platform)){
            if(this.code.equals(BetType.POINTIN)){
                betAmount = new BigDecimal(this.money);
            }
        }else{
            // booming platform
            betAmount = new BigDecimal(this.betinfo);
        }

        return betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal winAmount = null;

        List<String> BoomingPlatform = Arrays.asList(PlatformType.BOOMING, PlatformType.BOOMINGLATAM);

        // not booming platform
        if(!BoomingPlatform.contains(this.platform)){
            if (this.code.equals(BetType.POINTOUT)) {
                winAmount = new BigDecimal(this.money);
            }
        }else{
            // booming platform
            winAmount = this.code.equals(BetType.POINTIN) ? new BigDecimal(this.betinfo - this.money) : new BigDecimal(this.betinfo + this.getMoney());
        }

        return winAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        BigDecimal turnover = null;

        List<String> BoomingPlatform = Arrays.asList(PlatformType.BOOMING, PlatformType.BOOMINGLATAM);

        // not booming platform
        if(!BoomingPlatform.contains(this.platform)){
            if(this.code.equals(BetType.POINTIN)){
                turnover = new BigDecimal(this.money);
            }
        }else{
            //booming
            turnover = new BigDecimal(this.betinfo);
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
        Integer status = 0;

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            if(this.betinfo == 0.00){
                status = 1;
            }
        }

        return status;
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
