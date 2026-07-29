package com.nextgen.gameaggregator.vendor.digitain.api.betandresult;

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
public class BetAndResultRequest {
    @NotNull
    @JsonProperty("prid")
    private Integer prid;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("tkn")
    private String tkn;

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
    @JsonProperty("rid")
    private String rid;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("txid")
    private String txid;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("cid")
    private String cid;

    @NotNull
    @DecimalMin(value = "0.0")
    @JsonProperty("bam")
    private BigDecimal bam;

    @NotNull
    @DecimalMin(value = "0.0")
    @JsonProperty("wam")
    private BigDecimal wam;

    @NotNull
    @JsonProperty("inf")
    private Inf info;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Inf {

        @NotNull
        @JsonProperty("isb")
        private boolean isb;

        @NotNull
        @JsonProperty("bot")
        private Integer bot;

        @NotNull
        @JsonProperty("wot")
        private Integer wot;

        @JsonProperty("rndf")
        private boolean rndf;
    }
}
