package com.nextgen.gameaggregator.vendor.spribe.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;

import lombok.Data;

@Data
public class ErrorVo {

  private Integer code;
  private String message;

  @JsonIgnore
  private ErrorCodes errorCodes;

  public void setErrorCode(ErrorCodes errorCodes) {
    this.errorCodes = errorCodes;
    this.code = errorCodes.code;
    this.message = errorCodes.description;
  }
}
