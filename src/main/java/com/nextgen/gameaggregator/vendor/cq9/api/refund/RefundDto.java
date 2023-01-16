package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto {
    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;
}
