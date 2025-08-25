package com.nextgen.gameaggregator.vendor.inout.api.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.api.authenticate.AuthenticateService;
import com.nextgen.gameaggregator.vendor.inout.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.inout.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.inout.api.settle.SettleService;
import com.nextgen.gameaggregator.vendor.inout.constant.Actions;
import com.nextgen.gameaggregator.vendor.inout.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {

    private final HttpService httpService;
    private final VendorService vendorService;
    private final AuthenticateService authenticateService;
    private final RefundService refundService;
    private final BetService betService;
    private final SettleService settleService;

    public GeneralAction(HttpService httpService,
                         VendorService vendorService,
                         AuthenticateService authenticateService,
                         RefundService refundService,
                         BetService betService,
                         SettleService settleService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.authenticateService = authenticateService;
        this.refundService = refundService;
        this.betService = betService;
        this.settleService = settleService;
    }

    @PostMapping
    public CommonVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        CommonVo vo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();

            CommonDto<GeneralActionDto> dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            ValidationUtils.validateRequest(dto);

            vo = this.actionHandling(httpRequestLog, request, dto);

        } catch (Exception e) {
            vo.setError(ResponseCode.INVALID_TOKEN);

        } finally {
            httpRequestLog.setRequestBody("Request Body: \n" + httpRequestLog.getRequestBody() + "\n\nRequest Header: \n" + vendorService.getHeaders(request));
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private CommonVo actionHandling(HttpRequestLog httpRequestLog, HttpServletRequest httpServletRequest, CommonDto<GeneralActionDto> commonDto)
            throws InvalidRequestException, InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        CommonVo vo = new CommonVo();
        String xSign = httpServletRequest.getHeader("x-request-sign");

        switch (commonDto.getAction()) {

            case Actions.INITIALIZE:
                vo = authenticateService.initSession(httpRequestLog, xSign);
                break;

            case Actions.BET:
                vo = betService.bet(httpRequestLog, xSign);
                break;

            case Actions.WITHDRAW:
                vo = settleService.settle(httpRequestLog, xSign);
                break;

            case Actions.ROLLBACK:
                vo = refundService.refund(httpRequestLog, xSign);
                break;

            default:
                vo.setError(ResponseCode.INVALID_TOKEN);
                break;

        }
        return vo;

    }
}
