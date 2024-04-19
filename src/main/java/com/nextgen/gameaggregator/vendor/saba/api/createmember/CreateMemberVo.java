package com.nextgen.gameaggregator.vendor.saba.api.createmember;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CreateMemberVo {
    @SerializedName("error_code")
    private Integer errorCode;

    @SerializedName("message")
    private String message;
}
