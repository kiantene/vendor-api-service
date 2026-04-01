package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromoWinRequest {
    @NotNull
    @JsonProperty("prid")
    private Integer prid;

    @NotBlank
    @JsonProperty("pid")
    private String pid;

    //testing vendor giving integer
    @NotBlank
    @Size(max = 255)
    @JsonProperty("gid")
    private String gid;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("opt")
    private String opt;

    @NotNull
    @DecimalMin(value = "0.0")
    @JsonProperty("pwa")
    private BigDecimal pwa;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("cid")
    private String cid;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("txid")
    private String txid;

}
