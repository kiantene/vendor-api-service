package com.nextgen.gameaggregator.vendor.topbet.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {

    @NotBlank
    @Size(max = 255)
    private String pid;

    @NotBlank
    @Size(max = 255)
    private String account;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("trans_id")
    private String transId;

    @NotNull
    private Long time;

    @NotBlank
    @Size(max = 255)
    private String sign;
}
