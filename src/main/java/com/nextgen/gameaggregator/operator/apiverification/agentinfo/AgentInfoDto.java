package com.nextgen.gameaggregator.operator.apiverification.agentinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentInfoDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotNull( message = "numeric number only")
    @Positive( message = "numeric number only")
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer agentId;


}
