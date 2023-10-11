package com.nextgen.gameaggregator.vendor.ifg.api.rollback;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import lombok.Data;

@Data
public class RollBackServiceVo extends CommonVo {

    @JacksonXmlProperty(localName = "refund")
    private RefundVo refund;
}
