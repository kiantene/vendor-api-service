package com.nextgen.gameaggregator.vendor.aasexyv2.api.endround;

import com.nextgen.gameaggregator.vendor.aasexyv2.vo.ResponseVo;
import lombok.Data;

@Data
public class SettleVo extends ResponseVo {
    //    private String status;
    private String userId;
    private String balanceTs;

}
