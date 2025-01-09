package com.nextgen.gameaggregator.vendor.aglive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgLiveGameUrlVo implements GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private String data;

    @JacksonXmlProperty(isAttribute = true, localName = "info")
    private String infoCheckAndCreate;

    @JacksonXmlProperty(localName = "ResponseCode")
    private String infoSessionToken;

    @JacksonXmlProperty(isAttribute = true, localName = "msg")
    private String msg;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}