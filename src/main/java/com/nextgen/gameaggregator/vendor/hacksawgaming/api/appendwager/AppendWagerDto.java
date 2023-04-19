package com.nextgen.gameaggregator.vendor.hacksawgaming.api.appendwager;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AppendWagerDto {

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
    @Digits(integer = 16, fraction = 2)
    public BigDecimal amount;

    @NotBlank
    @Digits(integer = Integer.MAX_VALUE, fraction = 0)
    public Integer gameId;

    @NotBlank
    @Size(max = 50)
    public String gameName;

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
    @Size(max = 100)
    public Integer description;

    @NotNull
    // 0= Unfinished, 1= Round Finish
    public Boolean isEndround;
}
