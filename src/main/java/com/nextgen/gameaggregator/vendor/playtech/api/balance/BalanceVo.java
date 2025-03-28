package com.nextgen.gameaggregator.vendor.playtech.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonBalanceVo;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo extends CommonVo {

    private CommonBalanceVo balance;
}
