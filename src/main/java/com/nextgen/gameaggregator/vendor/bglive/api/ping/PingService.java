package com.nextgen.gameaggregator.vendor.bglive.api.ping;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import org.springframework.stereotype.Service;

@Service
public class PingService {
    private final HttpService httpService;

    public PingService(HttpService httpService) {
        this.httpService = httpService;
    }

    public CommonVo ping(HttpRequestLog httpRequestLog) {
        CommonVo commonVo = new CommonVo();
        try {
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = HttpService.convertJsonToDto(body, CommonDto.class);
            // Handle the action and return the resulting value
            this.doValidation(commonDto);
            Long time = System.currentTimeMillis();
            commonVo.setSuccessResponse(commonDto.getId(), time);


        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        }
        return commonVo;
    }

    private void doValidation(CommonDto commonDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
    }
}
