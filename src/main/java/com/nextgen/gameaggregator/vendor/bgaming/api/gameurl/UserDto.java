package com.nextgen.gameaggregator.vendor.bgaming.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private String id; // player username
}
