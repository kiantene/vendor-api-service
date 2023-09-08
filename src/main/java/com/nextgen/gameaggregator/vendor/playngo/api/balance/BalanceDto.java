package com.nextgen.gameaggregator.vendor.playngo.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "balance")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto extends CommonDto {

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;

    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[a-zA-Z]+$")
    @JacksonXmlProperty(localName = "currency")
    private String currency;

}
