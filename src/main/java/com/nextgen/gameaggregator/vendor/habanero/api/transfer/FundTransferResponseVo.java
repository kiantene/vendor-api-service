package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.habanero.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundTransferResponseVo extends CommonVo {

    @JsonProperty("status")
    private StatusVo statusVo;

    public FundTransferResponseVo() {
        this.setStatusVo(new StatusVo());
    }

}
