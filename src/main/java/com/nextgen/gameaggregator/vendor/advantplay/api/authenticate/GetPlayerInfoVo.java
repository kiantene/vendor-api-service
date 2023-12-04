package com.nextgen.gameaggregator.vendor.advantplay.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.vendor.advantplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class GetPlayerInfoVo extends ResponseVo {
    @JsonProperty("OPToken")
    private String opToken;
    private String brandCode;
    private String siteCode;
    private String playerId;
    private String playerName;
    private String playerCountry; // Optional
    private String currency;
    private Integer testAccount; // Optional
}
