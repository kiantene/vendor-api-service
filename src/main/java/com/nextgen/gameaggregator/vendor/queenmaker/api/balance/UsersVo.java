package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsersVo {
    private String userid;
    private Integer err;
    private String errdesc;
    private List<WalletsVo> wallets;

    public void setResponseCode(String errCode) {
        this.err = Integer.valueOf(errCode);
        this.errdesc = ResponseCode.RESPONSE_DESCRIPTION.get(errCode);
    }

    public void setResponseCode(String errCode, String errDesc) {
        this.err = Integer.valueOf(errCode);
        this.errdesc = errDesc;
    }
}
