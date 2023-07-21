package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataVo {

    @NotBlank(message = "url can not be blank")
    private String link;

    @SerializedName("session_id")
    private Integer sessionId;

}
