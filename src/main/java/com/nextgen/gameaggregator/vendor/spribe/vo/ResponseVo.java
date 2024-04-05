package com.nextgen.gameaggregator.vendor.spribe.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ResponseVo implements HttpResponse {

  private Integer code;
  private String message;
  private DataVo data;

  @JsonIgnore
  private ErrorCodes errorCodes;

  public void setErrorCode(ErrorCodes errorCodes) {
    this.errorCodes = errorCodes;
    this.code = errorCodes.code;
    this.message = errorCodes.description;
  }

  @Override
  public boolean hasError() {
    return !this.errorCodes.equals(ErrorCodes.SUCCESS);
  }
}
