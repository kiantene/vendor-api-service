package com.nextgen.gameaggregator.vendor.aasexy.api.rollback;

import com.nextgen.gameaggregator.vendor.aasexy.vo.ResponseVo;
import lombok.Data;

@Data
public class CancelVo extends ResponseVo {
    private String balanceTs;

}
