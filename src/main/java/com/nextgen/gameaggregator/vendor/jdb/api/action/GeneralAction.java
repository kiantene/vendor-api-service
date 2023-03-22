package com.nextgen.gameaggregator.vendor.jdb.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.jdb.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.jdb.api.cancelbet.CancelBetService;
import com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle.CancelBetNSettleService;
import com.nextgen.gameaggregator.vendor.jdb.api.endround.BetNSettleService;
import com.nextgen.gameaggregator.vendor.jdb.api.result.SettleService;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.dto.VendorRequestDto;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;

    // CQ9 Action Services
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BetNSettleService betNSettleService;
    @Autowired
    private BetService betService;
    @Autowired
    private CancelBetService cancelBetService;
    @Autowired
    private CancelBetNSettleService cancelBetNSettleService;
    @Autowired
    private SettleService settleService;

    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            VendorRequestDto commonDto = HttpService.convertQueryStringToDto(body, VendorRequestDto.class);
            ValidationUtils.validateRequest(commonDto);
            String params = VendorService.decrypt(commonDto.getX(), "47e0cd2ece0883e2", "b87f2867577b68ce");
            log.info(params);
            ActionDto actionDto = HttpService.convertJsonToDto(params, ActionDto.class);
            actionDto.setParams(params);
            vo = this.actionHandling(actionDto, traceId);

        } catch (InvalidDecryptionException ex) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException ex) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException ex) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (Exception ex) {
            vo.setResponseCode(ResponseCode.FAILED);
            log.error(ex.getMessage());
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException{
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId) {
        CommonVo vo = new CommonVo();
        switch (actionDto.getAction()) {
            case Actions.CANCEL_BET_AND_SETTLE:
                vo = cancelBetNSettleService.cancelBetNSettle(actionDto, traceId);
                break;
            case Actions.GET_BALANCE:
                vo = balanceService.balance(actionDto, traceId);
                break;
            case Actions.BET_AND_SETTLE:
                vo = betNSettleService.betNSettle(actionDto, traceId);
                break;
            case Actions.BET:
                vo = betService.bet(actionDto, traceId);
                break;
            case Actions.SETTLE:
                vo = settleService.settle(actionDto, traceId);
                break;
            case Actions.CANCEL_BET:
                vo = cancelBetService.cancelBet(actionDto, traceId);
                break;
            default:
                vo.setResponseCode(ResponseCode.INVALID_ACTION);
                break;
        }

        return vo;
    }
}
