package com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.vo;

import com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.constant.ConstantErrorMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ErrorVo {

    private String code = ConstantErrorMessage.UNEXPECTED_ERROR_CODE;

    private String message = ConstantErrorMessage.UNEXPECTED_ERROR;

    private Map<String, String> validation = new HashMap<String, String>();

    public ErrorVo() {
    }

    public ErrorVo(String code, String message, Map<String, String> validation) {
        this.code = code;
        this.message = message;
        this.validation = validation;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getValidation() {
        return validation;
    }

    public void setValidation(Map<String, String> validation) {
        this.validation = validation;
        if(!this.validation.isEmpty()){
            this.code = ConstantErrorMessage.INVALID_PARAM_CODE;
            this.message = ConstantErrorMessage.INVALID_PARAM;
        }
    }


}
