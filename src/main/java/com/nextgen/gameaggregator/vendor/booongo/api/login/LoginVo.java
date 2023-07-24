package com.nextgen.gameaggregator.vendor.booongo.api.login;

import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import lombok.Data;

@Data
public class LoginVo extends CommonVo {

    private LoginPlayerVo player;
    private BalanceVo balance;
    private String tag;
}
