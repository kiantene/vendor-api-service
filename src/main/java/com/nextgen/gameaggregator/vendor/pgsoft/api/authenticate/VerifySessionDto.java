package com.nextgen.gameaggregator.vendor.pgsoft.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VerifySessionDto extends CommonDto {
    @NotBlank
    @Size(min = 1, max = 100)
    private String operatorPlayerSession;

    //* Below are not mandatory
    private String ip;
    private String customParameter;
    @NotNull
    @Range(min = 0)
    private Integer gameId;

}


