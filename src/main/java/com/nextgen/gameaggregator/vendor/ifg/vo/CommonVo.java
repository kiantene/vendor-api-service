package com.nextgen.gameaggregator.vendor.ifg.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "service")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {

    @JacksonXmlProperty(isAttribute = true)
    private String session;

    @JacksonXmlProperty(isAttribute = true)
    private String time;

    @JacksonXmlProperty(localName = "error")
    private ErrorVo error;

    @Override
    public boolean hasError() {

        if(error != null){
            return true;
        }else{
            return false;
        }
    }

}
