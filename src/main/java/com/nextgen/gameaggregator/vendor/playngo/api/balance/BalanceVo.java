package com.nextgen.gameaggregator.vendor.playngo.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.vo.CommonVo;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "balance")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo extends CommonVo {
}
