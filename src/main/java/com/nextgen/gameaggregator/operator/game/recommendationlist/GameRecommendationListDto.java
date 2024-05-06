package com.nextgen.gameaggregator.operator.game.recommendationlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameRecommendationListDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36)
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;
    
    @NotNull(message = "at least 1 date range must be specified: weekly / monthly")
    @Size(min = 5, max = 10, message = "min 5 and max 20 alphanumeric underscore")
    private String dateRangeType;

    @Size(min = 2, max = 20, message = "min 2 and max 20 alphanumeric underscore")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_COLON_REGEX, message = "min 2 and max 20 alphanumeric underscore") // Only alphanumeric allowed
    private String vendorCode;
    
    @NotNull(message = "at least 1 currency must be selected")
    @Size(min = 3, max = 10, message = "min 3 and max 10  characters")
    private String currency;
    
    @NotNull(message = "at least 1 criteria must be specified: hot / top")
    @Size(min = 3, max = 3)
    private String type;

}
