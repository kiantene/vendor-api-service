package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDetailDto {

    @JsonProperty("name")
    public String name;

    @JsonProperty("keyname")
    public String keyName;

    @JsonProperty("gametypeid")
    public Integer gameTypeId;

    @JsonProperty("gametypename")
    public String gameTypeName;

    @JsonProperty("brandgameid")
    public String brandGameId;

    @JsonProperty("gamesessionid")
    public String gameSessionId;

    @JsonProperty("gameinstanceid")
    public String gameInstanceId;

    @JsonProperty("friendlygameinstanceid")
    public String friendlyGameInstanceId;

    @JsonProperty("maxpaylimit")
    public BigDecimal maxpayLimit;

    @JsonProperty("channel")
    public Integer channel;

    @JsonProperty("device")
    public String device;

    @JsonProperty("browser")
    public String browser;

    @JsonProperty("productexternalid")
    public String productExternalId;

    @JsonProperty("srij_smdata")
    public String srijSmData;
}
