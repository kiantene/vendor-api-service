package com.nextgen.gameaggregator.vendor.aasexy.api.endround;

import com.nextgen.gameaggregator.vendor.aasexy.vo.ResponseVo;
import lombok.Data;

@Data
public class SettleVo extends ResponseVo {
    //    private String status;
    private String userId;
    private String balanceTs;

}
