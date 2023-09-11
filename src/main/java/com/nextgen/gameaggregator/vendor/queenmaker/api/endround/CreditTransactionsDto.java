package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.dto.JpcontribsDto;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditTransactionsDto implements BetResultData {

    @NotBlank(message = "userid cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid userid Format")
    @Size(min = 1, max = 50, message = "Invalid userid Size")
    private String userid;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid authtoken Format")
    @Size(min = 1, max = 256, message = "Invalid authtoken Size")
    private String authtoken;

    private String brandcode;

    @NotNull(message = "amt cannot be empty")
    @Range(min = 0, message = "amt cannot less than 0")
    @Digits(integer = 12, fraction = 6, message = "Invalid amt Format")
    private BigDecimal amt;

    @NotBlank(message = "cur cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid cur Format")
    @Size(min = 3, max = 8, message = "Invalid cur Size")
    private String cur;

    private String ipaddress; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid ptxid Format")
    @Size(min = 1, max = 36, message = "Invalid ptxid Size")
    private String ptxid;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid refptxid Format")
    @Size(min = 1, max = 36, message = "Invalid refptxid Size")
    private String refptxid;

    @NotNull(message = "txtype cannot be empty")
    @Range(min = 0, message = "Invalid txtype Format")
    private Integer txtype;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}", message = "Invalid timestamp Format")
    @Size(min = 1, max = 36, message = "Invalid timestamp Size")
    private String timestamp;

    private Integer platformtype;

    @NotBlank(message = "gpcode cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid gpcode Format")
    @Size(min = 1, max = 50, message = "Invalid gamecode Size")
    private String gpcode;

    @NotBlank(message = "gamecode cannot be empty")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "Invalid gamecode Format")
    @Size(min = 1, max = 50, message = "Invalid gamecode Size")
    private String gamecode;

    private String gamename;
    private Integer gametype;
    private String externalgameid;
    private String roundid;
    private String externalroundid;
    private String betid; // optional
    private String externalbetid; // optional
    private String senton;
    private Boolean isclosinground;
    private BigDecimal ggr;

    @NotNull(message = "turnover cannot be empty")
    @Range(min = 0, message = "turnover cannot less than 0")
    @Digits(integer = 12, fraction = 6, message = "Invalid turnover Format")
    private BigDecimal turnover;

    private BigDecimal unsettledbets;
    private String walletcode; // optional
    private Integer bonustype; // optional
    private String bonuscode; // optional
    private String desc; // optional
    private String jpexternalid; // optional
    private String jpcur; // optional
    private BigDecimal jprate;
    private BigDecimal jpamt;
    private BigDecimal jpcvtamt;
    private BigDecimal jpbal;
    private List<JpcontribsDto> jpcontribs;
    private BigDecimal commission; // optional
    private String redeemcode; // optional

    @Override
    public String getExternalTransactionId() {
        return this.ptxid;
    }

    @Override
    public String getVendorBetId() {
        return this.ptxid;
    }

    @Override
    public String getRoundId() {
        return this.refptxid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amt;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.turnover;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return VendorService.convertToTimestamp(this.timestamp);
    }

    @Override
    public Long getVendorSettleTime() {
        return VendorService.convertToTimestamp(this.timestamp);
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
