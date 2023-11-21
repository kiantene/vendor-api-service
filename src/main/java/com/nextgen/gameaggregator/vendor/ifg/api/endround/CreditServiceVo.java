package com.nextgen.gameaggregator.vendor.ifg.api.endround;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import lombok.Data;

@Data
public class CreditServiceVo extends CommonVo {

    @JacksonXmlProperty(localName = "roundwin")
    private RoundWinVo roundwin;

    @Override
    public boolean hasError() {
        // check the error variable to decide it is error or not
        if(roundwin.getError() != null){
            return true;
        }else{
            return false;
        }
    }
}
