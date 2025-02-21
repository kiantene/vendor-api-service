package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrdersMapDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("orderId")
    private String orderId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("loginId")
    private String loginId;
}
