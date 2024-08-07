package com.nextgen.gameaggregator.vendor.dotconnections.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EndWagerDto extends CommonDto implements BetResultData {

    @NotNull
    @Digits(integer = 20, fraction = 8, message = ResponseCodes.INVALID_AMOUNT)
    public BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$") // not allow whitespace
    public String roundId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$")
    public String wagerId;

    @NotBlank
    @Size(max = 255)
    public String provider;

    @Nullable
    public String gameResult;

    @NotNull
    @Pattern(regexp = "^true$|^false$")
    // 0= Unfinished, 1= Round Finish
    public String isEndround;

    public BetStatus betStatus;

    @Override
    public String getExternalTransactionId() {
        return this.wagerId;
    }

    @Override
    public String getVendorBetId() {
        return this.wagerId;
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
        return this.amount;
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
        return getCurrentTimeStamp();
    }

    @Override
    public Long getResultTime() {
        return getCurrentTimeStamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return getCurrentTimeStamp();
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
        // Default end wager as unsettled
        BetStatus betStatus = BetStatus.UNSETTLED;

        // If round ended then set to settle
        if (this.isEndround.equals("true")) {
            betStatus = BetStatus.SETTLED;
        }

        return betStatus;

    }
}
