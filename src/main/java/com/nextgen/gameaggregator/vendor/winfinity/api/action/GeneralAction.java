package com.nextgen.gameaggregator.vendor.winfinity.api.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.winfinity.api.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.winfinity.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.winfinity.api.bet.PayinService;
import com.nextgen.gameaggregator.vendor.winfinity.api.clearmastersession.ClearMasterSessionService;
import com.nextgen.gameaggregator.vendor.winfinity.api.clearsession.ClearSessionService;
import com.nextgen.gameaggregator.vendor.winfinity.api.endround.EndroundService;
import com.nextgen.gameaggregator.vendor.winfinity.api.result.PayoutService;
import com.nextgen.gameaggregator.vendor.winfinity.api.rollback.RefundService;
import com.nextgen.gameaggregator.vendor.winfinity.api.tips.TipsService;
import com.nextgen.gameaggregator.vendor.winfinity.constant.Commands;
import com.nextgen.gameaggregator.vendor.winfinity.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.winfinity.service.VendorService;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private PayinService payinService;
    @Autowired
    private PayoutService payoutService;
    @Autowired
    private AuthenticateService authenticateService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private EndroundService endroundService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private TipsService tipsService;
    @Autowired
    private ClearSessionService clearSessionService;
    @Autowired
    private ClearMasterSessionService clearMasterSessionService;

    // Handle incoming API requests
    @PostMapping(path = { "", "/{qa}" })
    public ResponseVo handleApiCall(HttpServletRequest request, @PathVariable(required = false) String qa) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Get the endpoint as a string
            String endpoint = request.getRequestURI();

            // Remove the leading "/"
            String correct_endpoint = endpoint.substring(1);

            // Remove qa path before get vendor line id
            if (qa != null) {
                int index = correct_endpoint.indexOf("/qa");
                if (index != -1) {
                    correct_endpoint = correct_endpoint.substring(0, index);
                }
            }

            // Get the vendor line id
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(EndPoints.PATH, correct_endpoint);

            // Decode request body
            String decodedBody = vendorService.decodeRequestBody(vendorLineId, body, qa, httpRequestLog);

            // Convert the request body into commonDto object
            CommonDto commonDto = HttpService.convertJsonToDto(decodedBody, CommonDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            // Handle commands
            vo = commandsSwitching(commonDto.getCom(), commonDto, traceId, decodedBody, httpRequestLog);

        } catch (JsonProcessingException | InvalidRequestException badRequestException) {
            httpService.logError(httpRequestLog, badRequestException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private ResponseVo commandsSwitching(Integer command, CommonDto commonDto, String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        switch (command) {
            case Commands.GET_BALANCE -> vo = balanceService.getBalance(commonDto, traceId, httpRequestLog);
            case Commands.PAYIN -> vo = payinService.payin(traceId, body, httpRequestLog);
            case Commands.PAYOUT -> vo = payoutService.payout(traceId, body, httpRequestLog);
            case Commands.CANCEL -> vo = refundService.refund(traceId, body, httpRequestLog);
            case Commands.CLEAR_SESSION -> vo = clearSessionService.clearSession(traceId, body, httpRequestLog);
            case Commands.TIPS -> vo = tipsService.tips(traceId, body, httpRequestLog);
            case Commands.CLEAR_MASTER_SESSION -> vo = clearMasterSessionService.clearMasterSession(traceId, body, httpRequestLog);
            case Commands.REGISTER_SESSION -> vo = authenticateService.registerSession(commonDto, traceId, httpRequestLog);
            case Commands.ENDROUND -> vo = endroundService.endround(traceId, body, httpRequestLog);
            default -> {
                vo.setErrorVo(ErrorCodes.BAD_REQUEST);
            }
        }

        return vo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
