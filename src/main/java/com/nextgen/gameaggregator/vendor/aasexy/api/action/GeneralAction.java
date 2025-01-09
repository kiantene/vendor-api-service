package com.nextgen.gameaggregator.vendor.aasexy.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.aasexy.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.aasexy.api.canceltips.CancelTipsService;
import com.nextgen.gameaggregator.vendor.aasexy.api.endround.SettleService;
import com.nextgen.gameaggregator.vendor.aasexy.api.resettle.ResettleService;
import com.nextgen.gameaggregator.vendor.aasexy.api.rollback.CancelService;
import com.nextgen.gameaggregator.vendor.aasexy.api.tips.TipsService;
import com.nextgen.gameaggregator.vendor.aasexy.api.voidbet.VoidBetService;
import com.nextgen.gameaggregator.vendor.aasexy.api.voidsettle.VoidSettleService;
import com.nextgen.gameaggregator.vendor.aasexy.constant.Actions;
import com.nextgen.gameaggregator.vendor.aasexy.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import com.nextgen.gameaggregator.vendor.aasexy.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {
    private final HttpService httpService;
    private final BalanceService balanceService;
    private final BetService betService;
    private final SettleService settleService;
    private final CancelService cancelService;
    private final TipsService tipsService;
    private final CancelTipsService cancelTipsService;
    private final VoidBetService voidBetService;
    private final VoidSettleService voidSettleService;
    private final ResettleService resettleService;
    private final VendorService vendorService;

    @Autowired
    public GeneralAction(HttpService httpService,
                         BalanceService balanceService,
                         BetService betService,
                         SettleService settleService,
                         CancelService cancelService,
                         TipsService tipsService,
                         CancelTipsService cancelTipsService,
                         VoidBetService voidBetService,
                         VoidSettleService voidSettleService,
                         ResettleService resettleService,
                         VendorService vendorService) {
        this.httpService = httpService;
        this.balanceService = balanceService;
        this.betService = betService;
        this.settleService = settleService;
        this.cancelService = cancelService;
        this.tipsService = tipsService;
        this.cancelTipsService = cancelTipsService;
        this.voidBetService = voidBetService;
        this.voidSettleService = voidSettleService;
        this.resettleService = resettleService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.ACTION)
    public ResponseEntity<ResponseVo> action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        ResponseVo vo = new ResponseVo();
        int httpStatus = HttpStatus.SC_OK; //default is 200 status

        try {

            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);

            // set back request body to json format
            httpRequestLog.setRequestBody(body + " | jsonFormat:" + convertedJsonString);

            // Construct this vo for action handling purpose
            ActionDto dto = HttpService.convertJsonToDto(convertedJsonString, ActionDto.class);

            // Validate the actionDto object
            this.doValidation(dto);

            // Handle the action and return the resulting value
            vo = this.actionHandling(dto, traceId, httpRequestLog, request);
            
        } catch (InvalidRequestException e){
            vo.setResponseCodes(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.FAIL);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        if (vo.getHttpStatus() != null) {
            httpStatus = vo.getHttpStatus();
            vo.setHttpStatus(null);
        }

        return new ResponseEntity<>(vo, HttpStatusCode.valueOf(httpStatus));
    }

    private ResponseVo actionHandling(ActionDto dto, String traceId, HttpRequestLog httpRequestLog, HttpServletRequest request) throws InvalidRequestException {
        return switch (dto.getMessage().getAction()) {
            case Actions.BALANCE -> balanceService.balance(httpRequestLog, traceId);
            case Actions.BET -> betService.bet(httpRequestLog, request);
            case Actions.SETTLE -> settleService.settle(httpRequestLog, request);
            case Actions.CANCEL -> cancelService.cancel(httpRequestLog, request, traceId);
            case Actions.TIPS -> tipsService.tips(httpRequestLog, request);
            case Actions.VOID_BET -> voidBetService.voidBet(httpRequestLog, request);
            case Actions.VOID_SETTLE -> voidSettleService.voidSettle(httpRequestLog, request);
            case Actions.CANCEL_TIPS -> cancelTipsService.cancelTips(httpRequestLog, request, traceId);
            case Actions.RESETTLE -> resettleService.resettle(httpRequestLog, request);
            default -> throw new InvalidRequestException();
        };
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessage());
    }
}
