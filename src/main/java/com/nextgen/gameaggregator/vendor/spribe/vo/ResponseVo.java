package com.nextgen.gameaggregator.vendor.spribe.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ResponseVo implements HttpResponse {

  private DataVo data;
  private ErrorVo error;

  @Override
  public boolean hasError() {
    return false;
  }
}
