package com.nextgen.gameaggregator.vendor.hacksaw.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthVo extends ResponseVo {

    private String externalPlayerId;
    private String name; // Optional
    private String accountCurrency;
    private String externalSessionId; // Optional
    private String languageId; // Optional
    private String countryId; // Optional
    private String birthDate; // Optional
    private Integer registrationDate; // Optional
    private Integer brandId; // Optional
    private Integer gender; // Optional
    private Integer maxBetLevel; // Optional
}
