package com.nextgen.gameaggregator.vendor.dotconnections.api.rollback;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CancelWagerDto implements RollbackData {

    @NotBlank
    @Size(max = 7)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String brandId;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z0-9]*$")
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String roundId;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    public String wagerId;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[a-z]+$")
    public String provider;

    @NotNull
    @Range(min = 1, max = 2)
    // 1=cancelWager, 2=cancelEndWager
    public Integer wagerType;

    @NotNull
    @Pattern(regexp = "^true$|^false$")
    // 0= Unfinished, 1= Round Finish
    public String isEndround;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.wagerId);
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
