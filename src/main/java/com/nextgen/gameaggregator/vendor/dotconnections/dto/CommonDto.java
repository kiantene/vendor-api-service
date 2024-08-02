package com.nextgen.gameaggregator.vendor.dotconnections.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {

    @NotBlank
    @Size(max = 255)
    public String brandId;

    @NotBlank
    @Size(max = 1000)
    public String sign;

    @NotBlank
    @Size(max = 50)
    public String brandUid;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = "[a-zA-Z]+")
    public String currency;

    public Long getCurrentTimeStamp() {
        return Instant.now().toEpochMilli();
    }
}
