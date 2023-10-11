package com.nextgen.gameaggregator.vendor.ifg.api.balance;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import lombok.Data;

@Data
public class BalanceServiceVo extends CommonVo {
    @JacksonXmlProperty(localName = "getbalance")
    private GetbalanceVo getbalanceVo;

    @Override
    public boolean hasError() {
        // check the error variable to decide it is error or not
        if(getbalanceVo.getError() != null){
            return true;
        }else{
            return false;
        }
    }

}
