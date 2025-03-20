package com.nextgen.gameaggregator.vendor.aasexy.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    private String key;

    @NotNull
    private ActionMessageDto message;


}
