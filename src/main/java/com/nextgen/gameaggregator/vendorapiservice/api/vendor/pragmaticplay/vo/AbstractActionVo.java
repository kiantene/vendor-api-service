package com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.vo;

import com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.constant.ConstantErrorMessage;

public class AbstractActionVo {

    public Boolean status = false;

    public String traceId = "";

    public ErrorVo error;

    public AbstractActionVo() {
         this.error = new ErrorVo();
    }

    public AbstractActionVo(Boolean status, String traceId) {
         this.error = new ErrorVo();
         if (this.error.getValidation().isEmpty() && status == true) {
             this.error.setCode(ConstantErrorMessage.SUCCESS_CODE);
             this.error.setMessage(ConstantErrorMessage.SUCCESS_MESSAGE);
             this.status = true;
         } else {
              this.status = false;
         }
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
        if(this.error.getValidation().isEmpty() && status){
            this.error.setCode(ConstantErrorMessage.SUCCESS_CODE);
            this.error.setMessage(ConstantErrorMessage.SUCCESS_MESSAGE);
        }else{
            this.status = false;
        }
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public ErrorVo getError() {
        return error;
    }

    public void setError(ErrorVo error) {
        this.error = error;
    }

    public void verifyResponseValue() {
        System.out.println(this.error.getMessage().isBlank());
        System.out.println( this.error.getValidation().isEmpty() );
        if(this.error.getMessage().isBlank() && this.error.getValidation().isEmpty() ){
            this.status =true;
            this.error.setCode(ConstantErrorMessage.SUCCESS_CODE);
            this.error.setMessage(ConstantErrorMessage.SUCCESS_MESSAGE);
        }
    }
}