package com.nextgen.gameaggregator.vendor.jdb.api.balance;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class BalanceDto {

    private Integer action;

    @NotBlank
    @Size(min = 12, max = 12)
    private Long ts;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String currency;

    @JsonProperty("gType")
    private Integer gType;
}
