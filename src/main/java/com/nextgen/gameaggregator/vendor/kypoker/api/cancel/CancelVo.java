package com.nextgen.gameaggregator.vendor.kypoker.api.cancel;

import com.nextgen.gameaggregator.vendor.kypoker.vo.dObject;
import lombok.Data;

@Data
public class CancelVo extends dObject {

    private Integer status;
    private Integer code;

}
