package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FreeSpinResultDto extends CommonDto implements BetResultData {

    @NotNull
    @Digits(integer = 20, fraction = 8, message = ResponseCodes.INVALID_AMOUNT)
    public BigDecimal amount;

    @NotNull
    @Size(max = 255)
    public Integer gameId;

    @NotBlank
    @Size(max = 255)
    public String gameName;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$") // not allow whitespace
    public String roundId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$") // not allow whitespace
    public String wagerId;

    @NotBlank
    @Size(max = 255)
    public String provider;

//    @NotNull
//    @Pattern(regexp = "^true$|^false$")
    // 0= Unfinished, 1= Round Finish
    public String isEndround;

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
        return this.gameId.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.amount;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
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
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 1;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;

    }
}