package com.nextgen.gameaggregator.vendor.dotconnections.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.dotconnections.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CancelWagerDto extends CommonDto implements RollbackData {

    @NotBlank
    @Size(max = 255)
    public String roundId;

    @NotBlank
    @Size(max = 255)
    public String wagerId;

    @NotBlank
    @Size(max = 255)
    public String provider;

    @NotNull
    @Range(min = 1, max = 2)

    // 1=cancelWager, 2=cancelEndWager
    public Integer wagerType;
    public String isEndround;
    public BigDecimal adjustmentAmount;

    // For rollback
    @Override
    public String getRollbackId() {
        return String.valueOf(this.wagerId);
    }

    @Override
    public Long getVendorSettledTime() {
        return this.getCurrentTimeStamp();
    }
}
