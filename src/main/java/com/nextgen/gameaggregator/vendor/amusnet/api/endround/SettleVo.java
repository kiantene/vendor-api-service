package com.nextgen.gameaggregator.vendor.amusnet.api.endround;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.amusnet.vo.ResponseVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JacksonXmlRootElement(localName = "DepositResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettleVo extends ResponseVo {

}
