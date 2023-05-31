package com.nextgen.gameaggregator.vendor.bng.api.login;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.Data;

@Data
public class LoginVo extends CommonVo {

    private LoginPlayerVo player;
    private LoginBalanceVo balance;
    private String tag;
}
