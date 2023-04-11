package com.nextgen.gameaggregator.vendor.jdb.api.action;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.jdb.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.jdb.api.cancelbet.CancelBetService;
import com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle.CancelBetNSettleService;
import com.nextgen.gameaggregator.vendor.jdb.api.endround.BetNSettleService;
import com.nextgen.gameaggregator.vendor.jdb.api.result.SettleService;
import com.nextgen.gameaggregator.vendor.jdb.constant.*;
import com.nextgen.gameaggregator.vendor.jdb.dto.VendorRequestDto;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

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
    @Autowired
    private VendorLineService vendorLineService;

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

            // Validate request parameters (Non-database related)
            ValidationUtils.validateRequest(commonDto);

            // Get the key and iv value from table
            String key = vendorLineService.getCredentialValueByName(13, Credentials.KEY);
            String iv = vendorLineService.getCredentialValueByName(13, Credentials.IV);

            // Decrypt the 'X' field in the VendorRequestDto object using the key and iv values obtained earlier.
            String params = VendorService.decrypt(commonDto.getX(), key, iv);

            // Convert the params string to an ActionDto object
            ActionDto actionDto = HttpService.convertJsonToDto(params, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(actionDto);

            // Set params to be the decrypted 'X' value again
            actionDto.setParams(params);

            // Handle the action and return the resulting value
            vo = this.actionHandling(actionDto, traceId);

        } catch (InvalidDecryptionException invalidDecryptionException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException invalidRequestException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
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
