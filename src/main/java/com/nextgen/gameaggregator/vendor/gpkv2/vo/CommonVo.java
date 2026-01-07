package com.nextgen.gameaggregator.vendor.gpkv2.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.ResponseCodes;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private Integer code;
    private String player_id;
    private String balance;
    private Long timestamp;
    private String msg;

    public void setErrorResponse(ResponseCodes responseCodes) {
        this.code = responseCodes.getCode();
        this.msg = responseCodes.getMessage();
    }

    @Override
    public boolean hasError() {
        return !this.code.equals(ResponseCodes.SUCCESS.getCode());
    }
}
