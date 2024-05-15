package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
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
        Long betTime = null;

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            if(this.istips.equals("1")){
                // tips
                betTime = Long.parseLong(this.timestamp) * 1000;
            }else{
                // place bet
                if(this.finished.equals("0")){
                    betTime = Long.parseLong(this.timestamp) * 1000;
                }
            }
        }

        //turbo game
        if(this.platform.equals(PlatformType.TURBOGAME) || this.platform.equals(PlatformType.TURBOGAMELATAM)){
            if(this.dealid.contains("place") && this.finished == null && this.code.equals("2")){
                // place bet
                betTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            if((this.finished.equals("0") && this.code.equals("2")) || (this.finished.equals("1") && this.code.equals("2"))){
                // place bet or straightly lose
                betTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            if((this.finished.equals("0") && this.code.equals("2")) || (this.finished.equals("1") && this.code.equals("2"))){
                // place bet or straightly lose
                betTime = Long.parseLong(this.timestamp) * 1000;
            }
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
            if(this.istips.equals("1")){
                // tips
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }else{
                // settled
                if(this.finished.equals("1")){
                    settledTime = Long.parseLong(this.timestamp) * 1000;
                }
            }
        }

        //turbo game
        if(this.platform.equals(PlatformType.TURBOGAME) || this.platform.equals(PlatformType.TURBOGAMELATAM)){
            if(this.dealid.contains("settle") && this.finished.equals("1")){
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            if(this.finished.equals("1")){
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            if(this.finished.equals("1")){
                settledTime = Long.parseLong(this.timestamp) * 1000;
            }
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
            if(this.istips.equals("1")){
                // tips
                status = BetStatus.SETTLED;
            }else{
                //normal bet
                if(this.finished.equals("0")){
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
            if(this.finished.equals("0")){
                status = BetStatus.UNSETTLED;
            }else{
                status = BetStatus.SETTLED;
            }
        }

        //booming
        if(this.platform.equals(PlatformType.BOOMING) || this.platform.equals(PlatformType.BOOMINGLATAM)){
            if(this.finished.equals("0")){
                status = BetStatus.UNSETTLED;
            }else{
                status = BetStatus.SETTLED;
            }
        }

        return status;
    }
}
