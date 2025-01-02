//package com.nextgen.gameaggregator.vendor.poker365.vo;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
//import lombok.Data;
//
//@Data
//@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude(JsonInclude.Include.NON_NULL)
//public class ErrorVo {
//
//    @JsonProperty("status")
//    private String status;
//
//    @JsonProperty("msg")
//    private String msg;
//
//    public static ErrorVo from(ResponseCodes responseCode) {
//        ErrorVo error = new ErrorVo();
//        error.setStatus(responseCode.status);
//        error.setMsg(responseCode.message);
//        return error;
//    }
//}
