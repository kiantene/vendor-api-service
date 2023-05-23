package com.nextgen.gameaggregator.vendor.bng.api.logout;

import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import lombok.Data;

@Data
public class LogoutVo extends CommonVo {
    private String uid;
}
