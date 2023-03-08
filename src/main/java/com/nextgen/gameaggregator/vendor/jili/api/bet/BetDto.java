package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
public class BetDto {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String reqId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String token;
    @NotBlank
    private String currency;
    @NotBlank
    private Integer game;
    @NotBlank
    private BigInteger round;
    @NotBlank
    private BigInteger wagersTime;
    @NotBlank
    private BigDecimal betAmount;
    @NotBlank
    private BigDecimal winloseAmount;
    private boolean isFreeRound;
    private String userId;
    private BigInteger transactionId;
    private String platform;
    private Integer statementType;
    private Integer gameCategory;
}
