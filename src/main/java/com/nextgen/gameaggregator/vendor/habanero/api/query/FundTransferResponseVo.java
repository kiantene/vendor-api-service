package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundTransferResponseVo {

    @JsonProperty("status")
    private StatusVo statusVo;

}
