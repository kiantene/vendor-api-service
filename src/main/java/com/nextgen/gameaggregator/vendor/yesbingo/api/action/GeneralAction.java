package com.nextgen.gameaggregator.vendor.yesbingo.api.action;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.api.balance.BalanceAction;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Actions;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private BalanceAction balanceAction;

    @PostMapping(path = EndPoints.PATH)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {

            String body = httpRequestLog.getRequestBody();
            ActionDto dto = HttpService.convertJsonToDto(body, ActionDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            switch (dto.getAction()) {
                case Actions.BALANCE -> {
                    responseVo = balanceAction.balance(httpRequestLog, traceId, body);
                }
                // If the header does not match any of the expected values, return an error response
                default -> {
                    throw new InvalidRequestException();
                }
            }

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

}
