package com.nextgen.gameaggregator.vendor.saba.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceBetVo extends GeneralVo {
    private String refId;
    private String licenseeTxId;
}
