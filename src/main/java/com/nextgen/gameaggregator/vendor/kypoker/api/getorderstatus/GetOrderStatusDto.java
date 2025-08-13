package com.nextgen.gameaggregator.vendor.kypoker.api.getorderstatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GetOrderStatusDto {

    @NotBlank
    @Size(min = 1, max = 36)
    private String s;

    @NotBlank
    @Size(min = 1, max = 36)
    private String orderId;

    @NotBlank
    @Size(min = 1, max = 36)
    private String account;
}
