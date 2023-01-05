package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.Data;

@Data
public class GameUrlVendorResponseVo {
    private GameUrlVo data;
    private StatusVo status;
}
