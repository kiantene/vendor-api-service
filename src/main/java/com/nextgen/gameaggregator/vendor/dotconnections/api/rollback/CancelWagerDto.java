package com.nextgen.gameaggregator.vendor.dotconnections.api.rollback;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.adjustment.AdjustmentData;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CancelWagerDto extends CommonDto implements RollbackData, AdjustmentData {

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

    @NotNull
    @Range(min = 1, max = 2)
    // 1=cancelWager, 2=cancelEndWager
    public Integer wagerType;

//    @NotNull
//    @Pattern(regexp = "^true$|^false$")
    // 0= Unfinished, 1= Round Finish
    public String isEndround;

    public BigDecimal adjustmentAmount;

    // For rollback
    @Override
    public String getRollbackId() {
        return String.valueOf(this.wagerId);
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    // For adjustment
    @Override
    public String getVendorBetId() {
        return String.valueOf(this.wagerId);
    }

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.wagerId);
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getAdjustmentAmount() {
        return this.adjustmentAmount;
    }

    @Override
    public Long getTimestamp() {
        return getCurrentTimeStamp();
    }
}
