package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.api.endround.EndRoundService;
import com.nextgen.gameaggregator.vendor.pgsoft.api.result.ResultDto;
import com.nextgen.gameaggregator.vendor.pgsoft.api.result.ResultService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.BetTypes;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.time.Instant;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class CashTransferInOutAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private BetService betService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private EndRoundService endRoundService;

    @PostMapping(path = Endpoints.BET)
    public ResponseVo<CashTransferInOutVo> betRequest(WebRequestWrapper request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct Vo
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();
        Long now = Instant.now().toEpochMilli();

        try {

            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            CashTransferInOutDto dto = HttpService.convertQueryStringToDto(body, CashTransferInOutDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());

            // === Debugging Purpose ==============================================================================
//            String betType = VendorService.identifyBetType(dto);
//            System.out.println("============= Bet Type ================================================");
//            System.out.println(betType);
//            System.out.println("------ " + dto.getExternalTransactionId() + "-----------");
//            System.out.println(body);
//            System.out.println("=========================================================================");
            // === Debugging Purpose ==============================================================================

            // Vendor resent this bet for validation
            if (VendorService.isResentForValidate(dto)) {
                // TODO see how to handle this
            } else {

                // Only process as an BetRequest if it is an BetRequest
                if (VendorService.isBetRequest(dto)) {
                    betService.process(traceId, gameSession, dto, body);
                }

                // Has to process result regardless win or lose
                BetResultEvent betResultEvent = resultService.process(traceId, gameSession, body);

                // Only process as an EndRound if it is an EndRound
                if (VendorService.isRoundEnded(dto)) {
                    endRoundService.process(betResultEvent);
                }
            }

            // Only set data of parent response if nothing goes wrong
            CashTransferInOutVo responseVo = new CashTransferInOutVo();
            parentResponseVo.setData(responseVo);

            //* hardcoded response
            responseVo.setUpdatedTime(now);
            responseVo.setBalanceAmount(walletService.getBalance(traceId, gameSession));
            responseVo.setCurrencyCode(gameSession.getCurrencyCode());

        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));

        } catch (InsufficientBalanceException insufficientBalanceException) {
            parentResponseVo.setErrorCode(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET));

        } catch (BetNotFoundException e) {
            throw new RuntimeException(e);
        } catch (DuplicateExternalTransactionIdException betNotFoundException) {
            throw new RuntimeException(betNotFoundException);
        } catch (InvalidOperatorResponseException e) {
            throw new RuntimeException(e);
        } finally {
            httpService.end(httpRequestLog, parentResponseVo);
        }
//        System.out.println("=============================error =======================");
//        System.out.println(parentResponseVo.toString());
//        System.out.println("====================================================");
        return parentResponseVo;
    }

}
