package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.BetType;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.regex.Matcher;

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

    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String betid;

    // method for 7mojo while handle settled request
    public void setBetId(String query) {
        try {
            // Decode the URL-encoded query string
            String decodedQuery = URLDecoder.decode(query, "UTF-8");

            // Define the pattern to match the betid value
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("betid=\\[\"([^\"]+)\"\\]");
            Matcher matcher = pattern.matcher(decodedQuery);

            if (matcher.find()) {
                this.betid = matcher.group(1);
            } else {
                this.betid = null;
            }
        } catch (Exception e) {
            this.betid = null;
        }
    }

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
        return this.gameinfo;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.money;
    }

    @Override
    public BigDecimal getWinAmount() {
        BigDecimal winAmount = null;

        if (this.code.equals(BetType.POINTOUT)) {
            winAmount = this.money;
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

        if (this.code.equals(BetType.POINTIN)) {
            turnover = this.money;
        }

        return turnover;
    }

    @Override
    public Long getVendorBetTime() {
        Long betTime = null;
        //pushgaming
        if (this.platform.equals(PlatformType.PUSHGAMING) || this.platform.equals(PlatformType.PUSHGAMINGLATAM)) {
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

        //pushgaming
        if (this.platform.equals(PlatformType.PUSHGAMING) || this.platform.equals(PlatformType.PUSHGAMINGLATAM)) {
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

        //pushgaming
        if (this.platform.equals(PlatformType.PUSHGAMING) || this.platform.equals(PlatformType.PUSHGAMINGLATAM)) {
            if (this.finished != null && this.finished.equals(BetType.FINISHED)) {
                status = BetStatus.SETTLED;
            } else {
                if (this.code.equals(BetType.POINTIN)) {
                    status = BetStatus.UNSETTLED;
                    return status;
                }
                status = BetStatus.SETTLED;
            }
        }
        return status;
    }
}
