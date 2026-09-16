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

    /**
     * Game id. <b>Optional</b> — the Digitain spec states it is "sometimes not available, for example, in
     * tournament wins", which is exactly {@code opt} 85. It carried {@code @NotBlank} until ONEAPI-372, so
     * a tournament prize win with no {@code gid} failed bean validation before reaching the controller and
     * came back as {@code err: 999 GeneralError} — a valid payload rejected, with no clue as to why.
     *
     * <p>Typed String although the spec says integer; Jackson coerces either way and nothing parses it.
     */
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
