package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.api.endround.EndRoundService;
import com.nextgen.gameaggregator.vendor.pgsoft.api.result.ResultService;
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

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class CashTransferInOutAction {
    @Autowired
    private VendorPlayerRepository vendorPlayerRepository;

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
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
        BigDecimal balanceAmount = null;
        String currencyCode = null;

        try {

            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            CashTransferInOutDto dto = HttpService.convertQueryStringToDto(body, CashTransferInOutDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // Vendor resent this bet due to last attempt is failed
            if (VendorService.isResentForValidate(dto)) {

                // Check is BetRequest Or BetResult, have to process differently
                Boolean shouldReprocess = (VendorService.isBetRequest(dto)) ? betService.shouldReprocess(dto) : resultService.shouldReprocess(dto);

                if (shouldReprocess) {
                    // TODO - to process without GameSession
                    // Process the 3 in 1 request
                    // process(traceId, gameSession, dto, body);
                }

                // TODO - to get these without GameSession
                balanceAmount = BigDecimal.valueOf(0.00);
                currencyCode = "CNY";

            } else {

                // Only verify session when is not a resent bet
                // Not to verify session when is a resent bet is due to the game session of the resent bet might already expired

                // 2. Verify session token
                GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());
                // TODO - pt 3/4/5 to refactor ValidationUtil.validateEqual to throw custom exception class
                // 3. Verify VendorGameCode from request body is match with VendorGameCode from game session
                VendorService.validateVendorGameCode(dto.getGameId(), gameSession.getVendorGameCode());
                // 4. Verify VendorCurrencyCode from request body is match with session VendorCurrencyCode
                VendorService.validateVendorCurrencyCode(dto.getCurrencyCode(), gameSession.getVendorCurrencyCode());
                // 5. Validate VendorPlayerUsername from request body is match with session VendorPlayerUsername
                VendorService.validatePlayerUsername(gameSession.getVendorPlayerUsername(), dto.getPlayerName());

                // Process the 3 in 1 request
                process(traceId, gameSession, dto, body);

                balanceAmount = walletService.getBalance(traceId, gameSession);
                currencyCode = gameSession.getCurrencyCode();

            }

            // Only set data of parent response if nothing goes wrong
            CashTransferInOutVo responseVo = new CashTransferInOutVo();
            parentResponseVo.setData(responseVo);
            //
            responseVo.setUpdatedTime(now);
            responseVo.setBalanceAmount(balanceAmount);
            responseVo.setCurrencyCode(currencyCode);

        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));

        } catch (InsufficientBalanceException insufficientBalanceException) {
            parentResponseVo.setErrorCode(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET));

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            parentResponseVo.setErrorCode(ResponseCodes.BET_ALREADY_EXISTED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.BET_ALREADY_EXISTED));

        } catch (GameNotSupportedException gameNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.BET_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.BET_FAILED));

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.BET_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.BET_FAILED));

        } catch (BetNotFoundException betNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.NO_BET_EXISTS);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NO_BET_EXISTS));

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            parentResponseVo.setErrorCode(ResponseCodes.INTERNAL_SERVER_ERROR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INTERNAL_SERVER_ERROR));

        } catch (InvalidPlayerException invalidPlayerException) {
            parentResponseVo.setErrorCode(ResponseCodes.PLAYER_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.PLAYER_DOES_NOT_EXIST));

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_OPERATOR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_OPERATOR));

        } catch (BetResultNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            httpService.end(httpRequestLog, parentResponseVo);
        }

        return parentResponseVo;
    }

    /**
     * Process the request as a BetRequest/BetResult/EndRound
     * @param traceId
     * @param gameSession
     * @param dto
     * @param body
     * @throws InvalidAgentApiCredentialException
     * @throws InvalidRequestException
     * @throws BetNotFoundException
     * @throws DuplicateExternalTransactionIdException
     * @throws InsufficientBalanceException
     * @throws InvalidOperatorResponseException
     */
    public void process(String traceId, GameSession gameSession, CashTransferInOutDto dto, String body) throws InvalidAgentApiCredentialException, InvalidRequestException, BetNotFoundException, DuplicateExternalTransactionIdException, InsufficientBalanceException, InvalidOperatorResponseException, BetResultNotFoundException {

        // If this is a BetRequest, process it as a BetRequest.
        if (VendorService.isBetRequest(dto)) betService.process(traceId, gameSession, dto, body);

        // Every request contains a bet result, so it has to be processed as a BetResult.
        BetResultEvent betResultEvent = resultService.process(traceId, gameSession, body);

        // If this is an EndRound, process it as an EndRound.
        if (VendorService.isRoundEnded(dto)) endRoundService.process(betResultEvent);

    }

}
