package com.nextgen.gameaggregator.vendor.ifg.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetbalanceVo {

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    private String result;

    @JacksonXmlProperty(localName = "balance")
    private BalanceVo balance;

    @JacksonXmlProperty(localName = "error")
    private ErrorVo error;
}
