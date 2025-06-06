package com.nextgen.gameaggregator.vendor.aasexy.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionMessageDto {

    @NotBlank
    private String action;

    private String userId;
}
