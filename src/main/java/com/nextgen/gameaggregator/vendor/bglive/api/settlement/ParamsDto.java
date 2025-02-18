package com.nextgen.gameaggregator.vendor.bglive.api.settlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
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
    @JsonProperty("loginId")
    private String loginId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("sn")
    private String sn;

    @Size(max = 255)
    @JsonProperty("tranId")
    private String tranId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JacksonXmlProperty(localName = "amount")
    private BigDecimal amount;

    @JsonProperty("orders")
    private List<OrdersDto> orders;

}