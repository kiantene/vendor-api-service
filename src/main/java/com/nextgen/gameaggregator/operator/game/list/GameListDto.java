package com.nextgen.gameaggregator.operator.game.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameListDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;


    @NotBlank( message = "min 2 and max 20 alphanumeric")
    @Size(min = 2, max = 20, message = "min 2 and max 20 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "min 2 and max 20 alphanumeric") // Only alphanumeric allowed
    private String vendorCode;

    @NotNull( message = "numeric number only")
    @Positive
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer pageNo =1;

    @NotBlank(message = "2 alphanumeric")
    @Size(min = 2, max = 2, message = " 2 alphanumeric only")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "2 alphanumeric only") // Only alphanumeric allowed
    private String displayLanguage = "en";

    @NotNull(message = "min 1 and max 500 numeric number only")
    @Positive(message = "min 1 and max 500 numeric number only")
    @Range(min= 1, max= 500, message = "min 1 and max 500 numeric number only")
    private Integer pageSize = 500;

}
