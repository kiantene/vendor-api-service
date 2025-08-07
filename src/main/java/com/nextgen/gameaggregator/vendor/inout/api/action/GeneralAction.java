package com.nextgen.gameaggregator.vendor.inout.api.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class GeneralAction {

    private final HttpService httpService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final AuthenticateService authenticateService;
    private final BetService betService;
    private final SettleService settleService;
    private final RefundService refundService;
    private final GameSessionService gameSessionService;

    public GeneralAction(HttpService httpService,
                         VendorService vendorService,
                         VendorLineService vendorLineService,
                         AuthenticateService authenticateService,
                         BetService betService,
                         SettleService settleService,
                         RefundService refundService,
                         GameSessionService gameSessionService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.authenticateService = authenticateService;
        this.betService = betService;
        this.settleService = settleService;
        this.refundService = refundService;
        this.gameSessionService = gameSessionService;
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

    private CommonVo actionHandling(HttpRequestLog httpRequestLog, HttpServletRequest httpServletRequest, CommonDto<GeneralActionDto> commonDto) {
        CommonVo vo = new CommonVo();

        switch (commonDto.getAction()) {

            case Actions.INITIALIZE:
                vo = authenticateService.initSession(httpRequestLog, httpServletRequest);
                break;

            case Actions.BET:
                vo = betService.bet(httpRequestLog, httpServletRequest.getHeader("x-request-sign"));
                break;

            case Actions.WITHDRAW:
                vo = settleService.settle(httpRequestLog, httpServletRequest.getHeader("x-request-sign"));
                break;

            case Actions.ROLLBACK:
                vo = refundService.refund(httpRequestLog, httpServletRequest);
                break;

            default:
                break;

        }
        return vo;

    }
}
