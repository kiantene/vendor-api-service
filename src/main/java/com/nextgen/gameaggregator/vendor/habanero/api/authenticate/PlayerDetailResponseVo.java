package com.nextgen.gameaggregator.vendor.habanero.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.habanero.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDetailResponseVo extends CommonVo {

    @JsonProperty("status")
    private StatusVo statusVo;

    @JsonProperty("accountid")
    private String accountId;

    @JsonProperty("accountname")
    private String accountnName;

    public PlayerDetailResponseVo() {
        this.setStatusVo(new StatusVo());
    }

}
