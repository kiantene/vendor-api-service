package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceBetParlayVo extends GeneralVo {
    private List<PlaceBetParlayTxnsVo> txns;
}
