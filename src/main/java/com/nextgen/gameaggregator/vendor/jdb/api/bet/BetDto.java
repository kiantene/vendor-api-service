package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.Formats;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String action;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long ts;

    @NotBlank
    @Pattern(regexp = "^[0-9]+$")
    private String transferId;

    @NotBlank
    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;
    
    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private BigDecimal amount;

    @NotNull
    @Positive(message = ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE)
    private Long gameRoundSeqNo;

    @NotBlank
    @JsonProperty("mType")
    @Pattern(regexp = "^[0-9]+$")
    private String mType;

    private BigDecimal effectiveTurnover;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public String getRoundId() {
        // trim the provider's category for decide round id value
        String trimCategory = this.mType.toString().toString().substring(0, 2);

        if(trimCategory.equals(Formats.SPRIBE)){
            return this.transferId.toString();
        }else{
            return this.gameRoundSeqNo.toString();
        }
    }

    @Override
    public String getGameId() {
        return this.mType.toString();
    }

    @Override
    public String getVendorBetId() {
        return this.transferId.toString();
    }

    @Override
    public BigDecimal getBetAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return effectiveTurnover;
    }

    @Override
    public Long getVendorBetTime() {
        return ts;
    }

    @Override
    public Long getResultTime() {
        return ts;
    }

    @Override
    public Long getVendorSettleTime() {
        return ts;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
