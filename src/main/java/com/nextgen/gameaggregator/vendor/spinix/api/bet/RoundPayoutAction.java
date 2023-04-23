package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.events.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spinix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

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

            GameSession gameSession;
            if(dto.getUserToken() == null) {
                // Get game session
                // TODO: vendor has no intention to send user_token with value for fish game's win transaction record. Use user token from last game session first
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getUserId(), dto.getGameId());
            } else {
                // Verify session token
                gameSession = gameSessionService.verifyToken(dto.getUserToken());
            }

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

            if(betRecord != null && winRecord != null) {

                // process Bet + Win together
                BetWinDto betWinDto = new BetWinDto();
                betWinDto.setReqId(dto.getReqId());
                betWinDto.setRoundId(dto.getRoundId());
                betWinDto.setId(dto.getRoundId());
                betWinDto.setBetAmount(betRecord.getAmount().abs());
                betWinDto.setWinAmount(winRecord.getAmount());
                betWinDto.setValidTurnover(dto.getValidTurnover().abs());
                betWinDto.setGameId(dto.getGameId());
                betWinDto.setTimestamp(winRecord.getConvertedTimestamp());

                SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, betWinDto, body);

                // Set Balance
                roundPayoutDataWalletVo.setBalance(settledBetEvent.getLastBalance());

            } else {

                if(betRecord != null && betRecord.getType().equals("bet")) {
                    // Set necessary values to process bet record
                    BetDto betDto = new ObjectMapper().convertValue(dto, BetDto.class);
                    betDto.setReqId(dto.getReqId());
                    betDto.setRoundId(dto.getRoundId());
                    betDto.setId(betRecord.getId());
                    betDto.setAmount(betRecord.getAmount().abs());
                    betDto.setValidTurnover(dto.getValidTurnover().abs());
                    betDto.setGameId(dto.getGameId());
                    betDto.setTimestamp(betRecord.getConvertedTimestamp());

                    SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, betDto, body);

                    // Set Balance
                    roundPayoutDataWalletVo.setBalance(settledBetEvent.getLastBalance());
                    
                } else if (winRecord != null && winRecord.getType().equals("win")) {

                    // Set necessary values to process win record
                    WinDto winDto = new ObjectMapper().convertValue(dto, WinDto.class);
                    winDto.setReqId(dto.getReqId());
                    winDto.setRoundId(dto.getRoundId());
                    winDto.setId(winRecord.getId());
                    winDto.setAmount(winRecord.getAmount());
                    winDto.setValidTurnover(dto.getValidTurnover());
                    winDto.setGameId(dto.getGameId());
                    winDto.setTimestamp(winRecord.getConvertedTimestamp());

                    SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, winDto, body);

                    // Set Balance
                    roundPayoutDataWalletVo.setBalance(settledBetEvent.getLastBalance());

                } else if(cancelBet != null && cancelBet.getType().equals("cancelBet")) {
                    // Send refund to Operator
                    // TODO: to confirm whether to use id or round id for cancel bet
                    BetRefundEvent betRefundEvent = walletService.processRollback(traceId, dto.getRoundId(), gameSession, body);

                    // Set Balance
                    roundPayoutDataWalletVo.setBalance(betRefundEvent.getLastBalance());
                }

            }

            // Set Currency + RoundPayoutDataWalletVo + Status + req_id
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setStatus(HttpStatus.SC_OK);
            roundPayoutVo.setReqId(dto.getReqId());

        } catch(BetNotFoundException |
                DuplicateExternalTransactionIdException |
                RecordNotFoundException e
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
                 DateTimeParseException |
                 CurrencyNotSupportedException |
                 JsonProcessingException |
                 InsufficientBalanceException |
                 NullPointerException |
                 IllegalArgumentException e
        ) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
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

    private void doVerification(RoundPayoutDto dto,
                                List<RoundPayoutTransactionDto> roundPayoutTransactionDtoList,
                                GameSession gameSession,
                                String token,
                                Map<String, Object> body
    ) throws InvalidPlayerException,
            InvalidRequestException,
            GameNotSupportedException,
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            CurrencyNotSupportedException,
            InvalidVendorLineException,
            CredentialNotFoundException {

        // Verify signature
        String signatureKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE_KEY);
        if(!VendorService.isSameSignature(token, body, signatureKey)) {
            throw new InvalidVendorLineException();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // General validation
        for (RoundPayoutTransactionDto obj : roundPayoutTransactionDtoList) {
            formatter.parse(obj.getTimestamp());
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

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, dto.getUserId());

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
    }

}