package com.nextgen.gameaggregator.vendor.evolutionlive.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.BasicDto;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckDto extends BasicDto {
    @Pattern(regexp = "^(?:|[a-zA-Z0-9_-]+)$")
    @Size(max = 250)
    private String sid; // Player session token
    private ChannelDto channel;

}
