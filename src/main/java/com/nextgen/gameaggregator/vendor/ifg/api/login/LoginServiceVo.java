package com.nextgen.gameaggregator.vendor.ifg.api.login;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import lombok.Data;

@Data
public class LoginServiceVo extends CommonVo {
    @JacksonXmlProperty(localName = "enter")
    private EnterVo enter;

    @Override
    public boolean hasError() {
        // check the error variable to decide it is error or not
        if(enter.getError() != null){
            return true;
        }else{
            return false;
        }
    }
}
