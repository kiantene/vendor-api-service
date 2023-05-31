package com.nextgen.gameaggregator.vendor.queenmaker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JpcontribsDto {

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 255)
    private String jpexternalid;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 3, max = 8)
    private String jpcur;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 3)
    private BigDecimal jprate;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal jpamt;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal jpcvtamt;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 2)
    private BigDecimal jpbal;

}
