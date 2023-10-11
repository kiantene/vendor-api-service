package com.nextgen.gameaggregator.vendor.ifg.api.bet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import lombok.Data;

@Data
public class TransactionServiceVo extends CommonVo {

    @JacksonXmlProperty(localName = "roundbet")
    private RoundBetVo roundbet;

    @Override
    public boolean hasError() {
        // check the error variable to decide it is error or not
        if(roundbet.getError() != null){
            return true;
        }else{
            return false;
        }
    }
}
