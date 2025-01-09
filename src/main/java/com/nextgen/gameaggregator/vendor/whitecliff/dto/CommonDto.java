package com.nextgen.gameaggregator.vendor.whitecliff.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]*$")
    private String sid;

    @JsonIgnore
    private String secretKey;

    @NotNull
    @JsonProperty("user_id")
    @Digits(integer = 50, fraction = 0)
    private BigInteger userid;

    @NotNull
    @JsonProperty("prd_id")
    @Digits(integer = 50, fraction = 0)
    private BigInteger prdId;

}
