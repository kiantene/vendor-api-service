package com.nextgen.gameaggregator.vendor.advantplay.api.betdetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerTokenDto {

    private String Timestamp;

    private String Seq;

    private String BrandCode;

    private String SiteCode;
    
    @NotBlank(message = "Token needed to build game URL")
    private String Token;

}
