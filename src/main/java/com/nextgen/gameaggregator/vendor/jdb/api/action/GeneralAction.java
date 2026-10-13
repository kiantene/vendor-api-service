package com.nextgen.gameaggregator.vendor.jdb.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.WalletRequestLog;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle.CancelBetNSettleService;
import com.nextgen.gameaggregator.vendor.jdb.api.credit.JdbRefundController;
import com.nextgen.gameaggregator.vendor.jdb.api.credit.JdbSettleController;
import com.nextgen.gameaggregator.vendor.jdb.api.debit.JdbDebitController;
import com.nextgen.gameaggregator.vendor.jdb.api.endround.BetNSettleService;
import com.nextgen.gameaggregator.vendor.jdb.api.promo.JdbPromoPayoutHandler;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.dto.VendorRequestDto;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private HttpService httpService;

    // CQ9 Action Services
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BetNSettleService betNSettleService;
    @Autowired
    private JdbDebitController jdbDebitController;
    @Autowired
    private JdbRefundController jdbRefundController;
    @Autowired
    private CancelBetNSettleService cancelBetNSettleService;
    @Autowired
    private JdbSettleController jdbSettleController;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private JdbPromoPayoutHandler promoPayoutHandler;

    @PostMapping(path = EndPoints.ACTION + "/{id}")
    public CommonVo action(HttpServletRequest request, @PathVariable String id) {

        if (request instanceof ResettableRequestWrapper wrapped){
            Map<String, String> enriched = wrapped.getEnrichedFields();
            if (!enriched.isEmpty() && enriched.containsKey("action")){
                int action = Integer.parseInt(enriched.get("action"));
                if(Actions.PROMO_PAYOUT == action) {
                    return promoPayoutHandler.handlePromo(enriched).getBody();
                }
            }
        }

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // JDB creates this log and publishes it below, and it must be this object rather than a
        // framework one: the framework builds from logContext.getBody(), the raw encrypted body,
        // while the decrypted payload is set on this one further down. Declaring it lets the
        // debit/credit engine record the operator call on it without publishing a second row
        // (OVI-3176). Inert for the legacy actions, which set those fields directly.
        WalletRequestLog.declareVendorOwned(LogContextHolder.get(), httpRequestLog);

        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            VendorRequestDto commonDto = HttpService.convertQueryStringToDto(body, VendorRequestDto.class);

            // Validate request parameters (Non-database related)
            ValidationUtils.validateRequest(commonDto);

            // Get the first vendor line id from list
            Integer vendorLineId = vendorLineService.getVendorLineIdListByNameAndValue(Credentials.JDB_ID, id);
            VendorLine vendorLine = vendorLineService.getVendorLineById(vendorLineId);
            Optional.ofNullable(vendorLine).orElseThrow(InvalidVendorLineException::new);

            // Get the key and iv value with vendorLineId
            String key = vendorService.getCredentialWithKey(vendorLine, Credentials.KEY);
            String iv = vendorService.getCredentialWithKey(vendorLine, Credentials.IV);

            // Decrypt the 'X' field in the VendorRequestDto object using the key and iv values obtained earlier.
            String params = VendorService.decrypt(commonDto.getX(), key, iv);

            httpRequestLog.setRequestBody(params); // change the body value by using decrypted data

            // Convert the params string to an ActionDto object
            ActionDto actionDto = HttpService.convertJsonToDto(params, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(actionDto);

            // Set params to be the decrypted 'X' value again
            actionDto.setParams(params);
            actionDto.setHttpRequestLog(httpRequestLog);

            // Handle the action and return the resulting value
            vo = this.actionHandling(actionDto, traceId, httpRequestLog, request, vendorLine);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            httpService.logError(httpRequestLog, credentialNotFoundException);
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidDecryptionException invalidDecryptionException) {
            httpService.logError(httpRequestLog, invalidDecryptionException);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private CommonVo actionHandling(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog, HttpServletRequest request, VendorLine vendorLine) {
        CommonVo vo = new CommonVo();
        switch (actionDto.getAction()) {
            case Actions.CANCEL_BET_AND_SETTLE ->
                    vo = cancelBetNSettleService.cancelBetNSettle(actionDto, traceId, httpRequestLog);
            case Actions.GET_BALANCE ->
                    vo = balanceService.balance(actionDto, traceId, httpRequestLog, request, vendorLine);
            case Actions.BET_AND_SETTLE -> vo = betNSettleService.betNSettle(actionDto, traceId, httpRequestLog);
            case Actions.BET -> vo = jdbDebitController.handle(actionDto, traceId, httpRequestLog).getBody();
            case Actions.SETTLE -> vo = jdbSettleController.handle(actionDto, traceId, httpRequestLog).getBody();
            case Actions.CANCEL_BET -> vo = jdbRefundController.handle(actionDto, traceId, httpRequestLog).getBody();
            default -> vo.setErrorResponseCode(ResponseCode.INVALID_ACTION);
        }

        return vo;
    }
}