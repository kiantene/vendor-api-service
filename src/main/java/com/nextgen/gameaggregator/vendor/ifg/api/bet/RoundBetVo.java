package com.nextgen.gameaggregator.vendor.ifg.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundBetVo {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    private String result;

    @JacksonXmlProperty(localName = "balance")
    private BalanceVo balance;

    @JacksonXmlProperty(localName = "error")
    private ErrorVo error;
}
