package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParamsDto {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("random")
    private String random;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sign")
    private String sign;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sn")
    private String sn;
    
    @JsonProperty("orderMap")
    private List<OrdersMapDto> ordersMapDto;
}
