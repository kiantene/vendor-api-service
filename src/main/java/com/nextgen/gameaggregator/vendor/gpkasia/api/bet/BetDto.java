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

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String dealid;

    @NotBlank
    @Pattern(regexp = "[12]")
    private String code;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
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
        String gameId = this.gameinfo;

        if(this.gameinfo.equals("45")){
            gameId = "ubsp-demo";
        }

        return gameId;
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

//        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
//            if(this.code.equals("2") && this.finished.equals("1")){
//                return BigDecimal.valueOf(this.money * -1.00);
//            }
//        }

        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        BigDecimal turnover = null;

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            // end round with decrease code is lose
            if(this.code.equals("2") && this.finished.equals("1")){
                turnover = BigDecimal.valueOf(this.money);
            }

            // first round is place bet
            if(this.code.equals("2") && this.finished.equals("0")){
                turnover = BigDecimal.valueOf(this.money);
            }
        }

        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            // place bet or tips
            if(this.code.equals("2")){
                turnover = BigDecimal.valueOf(this.money);
            }
        }

        return turnover;
    }

    @Override
    public Long getVendorBetTime() {
        Long time = null;

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            if((this.finished.equals("0") && this.code.equals("2")) || (this.finished.equals("1") && this.code.equals("2"))){
                time = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            // tips
            if(this.istips.equals("1")){
                time = Long.parseLong(this.timestamp) * 1000;
            }else{
                //place bet
                if(this.code.equals("2")){
                    time = Long.parseLong(this.timestamp) * 1000;
                }
            }
        }

        return time;
    }

    @Override
    public Long getResultTime() {
        return VendorService.getMilSec();
    }

    @Override
    public Long getVendorSettleTime() {
        Long time = null;

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            // end round
            if(this.finished.equals("1")){
                time = Long.parseLong(this.timestamp) * 1000;
            }
        }

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            // if tips
            if(this.istips.equals("1")){
                time = Long.parseLong(this.timestamp) * 1000;
            }else{
                // if win game
                if(this.code.equals("1")){
                    time = Long.parseLong(this.timestamp) * 1000;
                }
            }
        }

        return time;
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

        //bgaming
        if(this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)){
            // not yet finish
            if(this.finished.equals("0")){
                status = BetStatus.UNSETTLED;
            }else{
                status = BetStatus.SETTLED;
            }
        }

        //7mojo
        if(this.platform.equals(PlatformType.SEVENMOJO) || this.platform.equals(PlatformType.SEVENMOJOLATAM)){
            if(this.istips.equals(1)){
                status = BetStatus.SETTLED;
            }else{
                if(this.getCode().equals("2")){
                    status = BetStatus.UNSETTLED;
                }else{
                    status = BetStatus.SETTLED;
                }
            }
        }

        return status;
    }
}
