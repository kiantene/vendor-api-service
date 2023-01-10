package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResettleDto {
    @NotBlank
    @Size(min = 1, max = 36)
    private String account;
    @NotBlank
    private String eventTime;
    @NotBlank
    @Size(min = 1, max = 36)
    private String gamehall;
    @NotBlank
    @Size(min = 1, max = 36)
    private String gamecode;
    @NotBlank
    @Size(min = 1, max = 30)
    private String roundid;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;
}
