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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends ActionDto implements BetResultData {

    @NotBlank
    @JsonProperty("api_token")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String apiToken;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user;

    @NotNull
    @PositiveOrZero
    private BigDecimal money;

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
    private BigDecimal betinfo;

    @NotBlank
    @Pattern(regexp = "\\d+")
    private String platform;

    @Pattern(regexp = "[01]")
    private String istips;

    @JsonProperty("root_roundid")
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String rootRoundid;

    @JsonProperty("root_dealid")
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String rootDealid;

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
            if(!(this.dealid.equals(this.rootDealid) && !(this.bRoundid.equals(this.rootRoundid)))){
                roundId = this.rootDealid;
            }else{
                roundId = this.dealid;
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

        List<String> boomingANDSpinomenalPlatform = Arrays.asList(PlatformType.BOOMING, PlatformType.BOOMINGLATAM, PlatformType.SPINOMENAL, PlatformType.SPINOMENALLATAM);

        // not booming or spinomenal platform
        if(!boomingANDSpinomenalPlatform.contains(this.platform)){
            if(this.code.equals(BetType.POINTIN)){
                betAmount = this.money;
            }
        }else{
            // booming & spinomenal platform
            betAmount = this.betinfo;
        }

        return betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal winAmount = null;

        List<String> boomingANDSpinomenalPlatform = Arrays.asList(PlatformType.BOOMING, PlatformType.BOOMINGLATAM, PlatformType.SPINOMENAL, PlatformType.SPINOMENALLATAM);

        // not booming or spinomenal platform
        if(!boomingANDSpinomenalPlatform.contains(this.platform)){
            if (this.code.equals(BetType.POINTOUT)) {
                winAmount = this.money;
            }
        }else{
            // booming & spinomenal platform
            winAmount = this.code.equals(BetType.POINTIN) ? this.betinfo.subtract(this.money) : this.betinfo.add(this.getMoney());
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

        List<String> boomingANDSpinomenalPlatform = Arrays.asList(PlatformType.BOOMING, PlatformType.BOOMINGLATAM, PlatformType.SPINOMENAL, PlatformType.SPINOMENALLATAM);

        // not booming platform
        if(!boomingANDSpinomenalPlatform.contains(this.platform)){
            if(this.code.equals(BetType.POINTIN)){
                turnover = this.money;
            }
        }else{
            //booming & spinomenal
            turnover = this.betinfo;
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

        //booming & spinomenal
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM) || this.platform.equals(PlatformType.SPINOMENAL) || this.platform.equals(PlatformType.SPINOMENALLATAM)){
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

        //booming & spinomenal
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM) || this.platform.equals(PlatformType.SPINOMENAL) || this.platform.equals(PlatformType.SPINOMENALLATAM)){
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

        //booming & spinomenal
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM) || this.platform.equals(PlatformType.SPINOMENAL) || this.platform.equals(PlatformType.SPINOMENALLATAM)){
            if(this.betinfo.compareTo(BigDecimal.ZERO) == 0){
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

        //booming & spinomenal
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM) || this.platform.equals(PlatformType.SPINOMENAL) || this.platform.equals(PlatformType.SPINOMENALLATAM)){
            status = BetStatus.SETTLED;
        }

        return status;
    }
}
