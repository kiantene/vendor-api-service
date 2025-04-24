package com.nextgen.gameaggregator.vendor.amusnet.api.betnsettle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.amusnet.vo.ResponseVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JacksonXmlRootElement(localName = "WithdrawAndDepositResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetNSettleVo extends ResponseVo {

}
