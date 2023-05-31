package com.nextgen.gameaggregator.vendor.bng.api.login;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.vo.BalanceVo;
import lombok.Data;

@Data
public class LoginVo extends CommonVo {

    private LoginPlayerVo player;
    private BalanceVo balance;
    private String tag;
}
