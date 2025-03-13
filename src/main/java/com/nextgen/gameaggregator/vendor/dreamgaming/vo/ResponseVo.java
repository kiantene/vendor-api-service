package com.nextgen.gameaggregator.vendor.dreamgaming.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseVo implements HttpResponse {

    private Integer codeId;

    private MemberVo member;

    public ResponseVo() {
        this.member = new MemberVo();
    }

    public void setCodeMsg(Integer code) {
        this.codeId = code;
    }

    @Override
    public boolean hasError() {
        return this.codeId != 0;
    }
}

