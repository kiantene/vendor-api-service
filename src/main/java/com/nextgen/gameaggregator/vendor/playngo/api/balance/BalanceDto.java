package com.nextgen.gameaggregator.vendor.playngo.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "balance")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends CommonDto {

    @NotBlank
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;
    @NotBlank
    @JacksonXmlProperty(localName = "productId")
    private String productId;
    @NotBlank
    @JacksonXmlProperty(localName = "currency")
    private String currency;
    @NotBlank
    @JacksonXmlProperty(localName = "gameId")
    private String gameId;
    @NotBlank
    @JacksonXmlProperty(localName = "accessToken")
    private String accessToken;
}
