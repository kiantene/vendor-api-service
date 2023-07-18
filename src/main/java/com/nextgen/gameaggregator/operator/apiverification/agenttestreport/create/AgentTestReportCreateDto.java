package com.nextgen.gameaggregator.operator.apiverification.agenttestreport.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentTestReportCreateDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotBlank(message = "min 3 and max 20 alphanumeric")
    @Size(min = 3, max = 20 , message = "min 3 and max 20 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX, message = "min 3 and max 20 alphanumeric only") // Only alphanumeric allowed
    private String username;

    @NotBlank(message = "min 3 and max 50 alphanumeric")
    @Size(min = 3, max = 50, message = "min 3 and max 50 alphanumeric")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = "min 3 and max 50 alphanumeric") // Only alphanumeric allowed
    private String gameCode;


}
