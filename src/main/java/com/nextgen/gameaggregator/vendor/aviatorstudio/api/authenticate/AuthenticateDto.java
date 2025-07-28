package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.aviatorstudio.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto extends CommonDto {

}
