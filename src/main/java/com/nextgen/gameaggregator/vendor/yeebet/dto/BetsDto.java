package com.nextgen.gameaggregator.vendor.yeebet.dto;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BetsDto {

    @NotBlank
    private String gameid;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private long createtime;

    @NotBlank
    private String serialnumber;

    @NotBlank
    private String userstatus;

    @NotBlank
    private String betpoint;

    @NotBlank
    private String betodds;

    @NotBlank
    private String userid;

    @NotBlank
    private String commamount;

    @NotBlank
    private String gameroundid;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private long settletime;

    private String gameresult;

    @NotBlank
    @Pattern(regexp = "[-+]?\\d*\\.?\\d+")
    private String winlost;

    @NotBlank
    private String gametype;

    @NotBlank
    private String gamestyle;

    @NotBlank
    private String currency;

    @NotBlank
    private String id;

    @NotBlank
    private String gameroundno;

    @NotBlank
    private String state;

    @NotBlank
    private String gameno;

    @NotBlank
    private String bettype;

    @NotBlank
    private String cid;

    @NotBlank
    // not allow negative numeric
    @Pattern(regexp = "\\d*\\.?\\d+")
    private String betamount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String foregift;
}