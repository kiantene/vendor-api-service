package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CashGetDto extends CommonDto {
    @NotBlank
    @Size(min = 3, max = 50)
    private String playerName;

    //* Below are not mandatory
    @NotBlank
    @Size(min = 1, max = 100)
    private String operatorPlayerSession;
    private Integer gameId;
}
