package com.nextgen.gameaggregator.operator.game.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameListDto {

    @NotBlank
    @Size(min = 36, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric allowed
    private String traceId;


    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String vendorCode;

    @NotNull
    @Positive
    @Range(min= 1, max= Integer.MAX_VALUE)
    private Integer pageNo;

    @NotNull
    @Positive
    @Range(min= 1, max= Integer.MAX_VALUE)
    private Integer size;

}
