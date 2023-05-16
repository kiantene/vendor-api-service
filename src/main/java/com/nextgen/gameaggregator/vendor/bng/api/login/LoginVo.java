package com.nextgen.gameaggregator.vendor.bng.api.login;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.Data;

@Data
public class LoginVo extends CommonVo {

    private String uid;
    private PlayerVo player;
    private BalanceVo balance;
    private String tag;
}
