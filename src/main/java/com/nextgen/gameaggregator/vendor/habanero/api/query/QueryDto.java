package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.dto.BaseGameDto;
import com.nextgen.gameaggregator.vendor.habanero.dto.SubAuthDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryDto implements BetResultData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @JsonProperty("type")
    public String type;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9:._-]+$")
    @JsonProperty("dtsent")
    public String dtSent;

    @JsonProperty("basegame")
    public BaseGameDto baseGame;

    @JsonProperty("auth")
    public SubAuthDto subAuth;

    @JsonProperty("queryrequest")
    public QueryRequestDto queryRequestDto;

    @Override
    public String getExternalTransactionId() {
        return this.getQueryRequestDto().getTransferId();
    }

    @Override
    public String getVendorBetId() {
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
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
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return null;
    }
}
