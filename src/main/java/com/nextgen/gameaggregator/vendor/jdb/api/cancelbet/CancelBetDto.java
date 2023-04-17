package com.nextgen.gameaggregator.vendor.jdb.api.cancelbet;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;

import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto {
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
    @Positive(message = "PARAMETER_CANNOT_BE_NEGATIVE")
    private BigDecimal amount;

    @Valid
    @NotNull
    @Size(min = 1, max = 30)
    private List<@NotNull Long> refTransferIds;

    @NotNull
    @Positive
    private Long gameRoundSeqNo;
}
