package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataDto {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String round_id;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String refund_round_id;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String action_id;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String refund_action_id;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String refund_callback_id;

    @Range(min = 0, max = 1)
    private Integer final_action;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    @Size(max = 4)
    private String currency;

    @NotBlank
    private String details;

    private DetailsDto detailsDto;
}
