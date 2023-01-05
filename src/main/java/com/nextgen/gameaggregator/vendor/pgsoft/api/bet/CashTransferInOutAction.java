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
import com.nextgen.gameaggregator.vendor.pgsoft.constant.BetTypes;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
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

    @PostMapping(path = Endpoints.BET)
    public ResponseVo<CashTransferInOutVo> betRequest(WebRequestWrapper request) {
        // Construct Vo
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();

        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        String traceId = UUID.randomUUID().toString();
        Long now = Instant.now().toEpochMilli();

        try {

            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            CashTransferInOutDto dto = HttpService.convertQueryStringToDto(body, CashTransferInOutDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // Emit event for additional asynchronous processing
            // TODO
//            switch () {
//              eventDispatcher.emit(WinClass.getClass(), body);
//              eventDispatcher.emit(LoseClass.getClass(), body);
//              eventDispatcher.emit(FreeSpinClass.getClass(), body);
//            }
            String betType = VendorService.identifyBetType(dto);
            System.out.println("============= Bet Type ================================================");
            System.out.println(betType);
            System.out.println("-----------------");
            System.out.println(body);
            System.out.println("=========================================================================");

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());
            // 4. Send bet request to Operator and check if player has enough balance
            switch (betType) {
                case BetTypes.REQUEST_AND_WIN_AND_END_ROUND: {
                    // Bet request
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);
                    EventDispatcherSystem.emitAsync(betEvent);
                    // Construct win dto
                    CashTransferInOut_WinDto winDto = HttpService.convertQueryStringToDto(body, CashTransferInOut_WinDto.class);
                    BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);
                    // Win
                    EventDispatcherSystem.emitAsync(betResultEvent);
                    // End round
                    EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));
                    break;
                }
                case BetTypes.REQUEST_AND_WIN_AND_ONGOING: {
                    // Bet request
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);
                    EventDispatcherSystem.emitAsync(betEvent);
                    // Construct win dto
                    CashTransferInOut_WinDto winDto = HttpService.convertQueryStringToDto(body, CashTransferInOut_WinDto.class);
                    BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);
                    // Win
                    EventDispatcherSystem.emitAsync(betResultEvent);
                }
                case BetTypes.REQUEST_AND_LOSE_AND_END_ROUND: {
                    // Bet request
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);
                    EventDispatcherSystem.emitAsync(betEvent);
                    // Construct win dto (with 0 win amount)
                    CashTransferInOut_WinDto winDto = HttpService.convertQueryStringToDto(body, CashTransferInOut_WinDto.class);
                    // End round
                    // TODO processLose
                    BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);
                    EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));
                    break;
                }
                case BetTypes.END_ROUND: {
                    BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());
                    // End round
                    EventDispatcherSystem.emitAsync(new EndRoundEvent(betHistory));
                    break;
                }
                case BetTypes.FREESPIN_WIN_AND_ONGOING: {
                    // Consturct win dto
                    CashTransferInOut_WinDto winDto = HttpService.convertQueryStringToDto(body, CashTransferInOut_WinDto.class);
                    BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);
                    // Win
                    EventDispatcherSystem.emitAsync(betResultEvent);
                    break;
                }
                case BetTypes.FREESPIN_LOSE_AND_ONGOING: {
                    break;
                }
                default:
                    break;

            }


//            // Emit event for additional asynchronous processing
//            EventDispatcherSystem.emitAsync(betEvent);
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
            if (parentResponseVo.getError() != null) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, parentResponseVo, traceId));
        }
        System.out.println("=============================error =======================");
        System.out.println(parentResponseVo.toString());
        System.out.println("====================================================");
        return parentResponseVo;
    }

}
