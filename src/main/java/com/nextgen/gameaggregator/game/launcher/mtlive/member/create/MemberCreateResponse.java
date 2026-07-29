package com.nextgen.gameaggregator.game.launcher.mtlive.member.create;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberCreateResponse {

    private String code;

    private String message;

    private String timestamp;

    private MemberCreateDataResponse data;

    public boolean isSuccess() {
        return code != null &&
                (code.equals(ResponseCode.SUCCESS.getCode())
                        || code.equals(ResponseCode.DATA_ALREADY_EXISTS.getCode()));
    }
}
