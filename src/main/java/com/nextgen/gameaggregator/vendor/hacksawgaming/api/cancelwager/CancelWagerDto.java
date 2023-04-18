package com.nextgen.gameaggregator.vendor.hacksawgaming.api.cancelwager;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CancelWagerDto {

    @NotBlank
    @Size(max =7)
    public String brandId;

    @NotBlank
    @Size(min = 32)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    public String sign;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Size(min = 3, max = 20)
    public String brandUid;

    @NotBlank
    @Size(min = 3, max =4)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    @NotBlank
    @Size(max = 64)
    public String roundId;

    @NotBlank
    @Size(max = 64)
    public String wagerId;

    @NotBlank
    @Size(max = 20)
    public String provider;

    @NotBlank
    @Pattern(regexp = "[12]")
    // 1=cancelWager, 2=cancelEndWager
    public Integer wagerType;

    @NotBlank
    @Pattern(regexp = "[01]")
    // 0= Unfinished, 1= Round Finish
    public Integer isEndround;
}
