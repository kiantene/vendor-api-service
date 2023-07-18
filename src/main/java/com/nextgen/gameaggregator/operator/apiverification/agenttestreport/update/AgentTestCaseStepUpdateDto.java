package com.nextgen.gameaggregator.operator.apiverification.agenttestreport.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentTestCaseStepUpdateDto {

    @NotBlank(message = "UUID format only")
    @Size(min = 36, max = 36, message = "UUID format only")
    @Pattern(regexp = ValidationUtils.UUID_REGEX, message = "UUID format only") // Only alphanumeric allowed
    private String traceId;

    @NotNull( message = "numeric number only")
    @Positive( message = "numeric number only")
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer masterCaseId;

    @NotNull( message = "numeric number only")
    @Positive( message = "numeric number only")
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer subCaseId;

    @NotNull( message = "numeric number only")
    @Positive( message = "numeric number only")
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer stepId;

    @NotNull(message = "long integer number only")
    @Positive(message = "long integer number only")
    @Range(min= 1659282428477L, max= Long.MAX_VALUE, message = "long integer number only")
    private Long startTime;

    @NotNull(message = "long integer number only")
    @Positive(message = "long integer number only")
    @Range(min= 1659282428477L, max= Long.MAX_VALUE, message = "long integer number only")
    private Long endTime;

    @NotBlank( message = "apiUrl required")
    @Size(min = 3, max = 2048, message = "min 3 and max 2048  characters")
    @Pattern(regexp = ValidationUtils.URL_REGEX, message = "URL format only")
    private String apiUrl;

    @NotBlank( message = "requestHeaders required")
//    @Size(min = 3, max = 2048, message = "min 3 and max 2048  characters")
    private String requestHeaders;

    @NotBlank( message = "requestBody required")
//    @Size(min = 3, max = 2048, message = "min 3 and max 2048  characters")
    private String requestBody;

    @NotNull( message = "numeric number only")
    @Positive( message = "numeric number only")
    @Range(min= 1, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer responseHttpCode;

    @NotBlank( message = "requestBody required")
//    @Size(min = 3, max = 2048, message = "min 3 and max 2048  characters")
    private String responseBody;

    @NotBlank( message = "expectedResponse required")
    private String expectedResponse;

    @NotNull( message = "numeric number only")
    //@Positive( message = "numeric number only")
    @Range(min= 0, max= Integer.MAX_VALUE, message = "numeric number only")
    private Integer status;

    @NotBlank( message = "messageCode required")
    private String messageCode;

   // @NotBlank( message = "remark required")
    @NotNull( message = "remark required")
    private String remark;
}
