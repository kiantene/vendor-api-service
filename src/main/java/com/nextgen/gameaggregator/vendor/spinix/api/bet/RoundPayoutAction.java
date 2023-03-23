package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spinix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RoundPayoutAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.ROUND)
    public RoundPayoutVo bet(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();
        String sign = request.getHeader("x-gaming-signature");
        String body = httpRequestLog.getRequestBody();
        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();

        try {

            // Convert original request body into dto
            RoundPayoutDto dto = HttpService.convertJsonToDto(body, RoundPayoutDto.class);
            ValidationUtils.validateRequest(dto);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto, sign);

            // Get game session
            // GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getUserId(), dto.getGameId());

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getUserToken());

            // Verify remaining parameters (Verify against database values)
            List<RoundPayoutTransactionDto> list = dto.getTransactionList();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> bodyObj = mapper.readValue(body, Map.class);
            this.doVerification(dto, list, gameSession, sign, bodyObj);

            // Search for bet, win and/or cancel bet
            RoundPayoutTransactionDto cancelBet = RoundPayoutDto.findTransaction(list, "cancelBet");
            RoundPayoutTransactionDto betRecord = RoundPayoutDto.findTransaction(list, "bet");
            RoundPayoutTransactionDto winRecord = RoundPayoutDto.findTransaction(list, "win");

            RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();

            // Set necessary values to process cancel bet record
            if(cancelBet != null && cancelBet.getType().equals("cancelBet")) {
                BigDecimal balance = walletService.getBalance(traceId, gameSession);

                // Set Balance
                roundPayoutDataWalletVo.setBalance(balance);
            } else {

                // Set necessary values to process bet record
                if(betRecord != null && betRecord.getType().equals("bet")) {
                    BetDto betDto = new ObjectMapper().convertValue(dto, BetDto.class);
                    betDto.setExternalTransactionId(betRecord.getId());
                    betDto.setAmount(betRecord.getAmount().abs());
                    betDto.setTimestamp(betRecord.getTimestamp());
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

                    // Set Balance
                    roundPayoutDataWalletVo.setBalance(betEvent.getLastBalance());
                }

                // Check if bet record exists before process win record
                BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

                // Set necessary values to process win record
                if (winRecord != null && betHistory.getRoundId() != null) {
                    WinDto winDto = new ObjectMapper().convertValue(dto, WinDto.class);
                    winDto.setExternalTransactionId(winRecord.getId());
                    winDto.setAmount(winRecord.getAmount());
                    winDto.setWinType(this.getWinType(winRecord));
                    winDto.setTimestamp(winRecord.getTimestamp());
                    winDto.setEffectiveTurnover(dto.getValidTurnover().abs());

                    BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);

                    // Emit event for additional asynchronous processing
                    if(winRecord.getIsEnd() == true) {
                        Thread.sleep(1000); // Sleep for 1 second
                        EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));
                    }

                    // Set Balance
                    roundPayoutDataWalletVo.setBalance(betResultEvent.getLastBalance());
                }

            }

            // Set Currency + RoundPayoutDataWalletVo + Status + req_id
            roundPayoutDataWalletVo.setCurrency(gameSession.getCurrencyCode());
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setStatus(HttpStatus.SC_OK);
            roundPayoutVo.setReqId(dto.getReqId());

        } catch(BetNotFoundException |
                DuplicateExternalTransactionIdException |
                BetResultNotFoundException |
                InterruptedException e
        ) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledVendorLineException |
                 DisabledAgentPlayerException e
        ) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException |
                 InvalidVendorLineException |
                 CredentialNotFoundException e
        ) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_TOKEN_NOT_FOUND_OR_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_UNAUTHORIZED);
            httpService.logError(httpRequestLog, e);
        } catch (GameNotSupportedException e) {
            roundPayoutErrorVo.setCode(ResponseCodes.GAME_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledGameException e) {
            roundPayoutErrorVo.setCode(ResponseCodes.GAME_NOT_AVAILABLE);
            roundPayoutVo.setStatus(HttpStatus.SC_FORBIDDEN);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 CurrencyNotSupportedException |
                 JsonProcessingException |
                 InsufficientBalanceException |
                 NullPointerException |
                 IllegalArgumentException e
        ) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if(roundPayoutVo.getStatus() == HttpStatus.SC_OK) {
                roundPayoutVo.setData(roundPayoutDataVo);
            } else {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
            httpService.end(httpRequestLog, roundPayoutVo);
        }

        return roundPayoutVo;
    }

    private void doValidation(RoundPayoutDto dto, String token) throws InvalidRequestException {
        Optional.ofNullable(token).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RoundPayoutDto dto, List<RoundPayoutTransactionDto> roundPayoutTransactionDtoList, GameSession gameSession, String token, Map<String, Object> body)
            throws InvalidPlayerException,
            InvalidRequestException,
            GameNotSupportedException,
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            CurrencyNotSupportedException,
            InvalidVendorLineException,
            CredentialNotFoundException {

        // General validation
        for (RoundPayoutTransactionDto obj : roundPayoutTransactionDtoList) {
            ValidationUtils.validateRequest(obj);
            switch(obj.getType()) {
                case "bet":
                    if(obj.getAmount().compareTo(BigDecimal.ZERO) > 0) throw new InvalidRequestException();
                    break;
                case "win":
                case "cancelBet":
                    if(obj.getAmount().compareTo(BigDecimal.ZERO) < 0) throw new InvalidRequestException();
                    break;
                default:
                    throw new InvalidRequestException();
            }
        }

        // Verify received username is the same from game session
        // ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);
        if(!gameSession.getVendorPlayerUsername().equals(dto.getUserId())) {
            throw new InvalidPlayerException();
        }

        String signatureKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE_KEY);
        if(!VendorService.isSameSignature(token, body, signatureKey)) {
            throw new InvalidVendorLineException();
        }

        // Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

    private WinType getWinType(RoundPayoutTransactionDto winRecord) {
        WinType winType;
        if (winRecord.getInfo().equals("feature_freespin")) {
            winType = WinType.WIN;
        } else {
            winType = (winRecord.getAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
        }

        return winType;
    }

}