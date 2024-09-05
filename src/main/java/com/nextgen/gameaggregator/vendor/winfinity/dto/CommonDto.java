package com.nextgen.gameaggregator.vendor.winfinity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @NotNull
    @Min(value = 0)
    @Max(value = 9)
    private Integer com;

    @Size(max = 32)
    private String tid;

    @NotBlank
    @Size(max = 32)
    private String msid;

    @Size(max = 24)
    private String tbid;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @Size(max = 4)
    private String cur;

    private String gtp;

    @Size(max = 32)
    private String sid;

    @Size(max = 32)
    private String gid;

    @PositiveOrZero
    private BigDecimal sum;

    private Long timestamp;

    @Size(max = 32)
    private String refid;
}
