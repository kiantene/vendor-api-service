package com.nextgen.gameaggregator.vendor.spinix.api.payout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spinix.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.spinix.api.cancel.CancelBetService;
import com.nextgen.gameaggregator.vendor.spinix.constant.*;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RoundPayoutAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private BetService betService;
    @Autowired
    private CancelBetService cancelBetService;

    @PostMapping(path = EndPoints.ROUND)
    public ResponseEntity<RoundPayoutVo> bet(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String sign = request.getHeader(EndPoints.HEADER_SIGNATURE);
        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            RoundPayoutDto dto = HttpService.convertJsonToDto(body, RoundPayoutDto.class);

            // Set request id
            String reqId = dto.getReqId();
            roundPayoutVo.setReqId(reqId);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto, sign);

            // Verify or get latest game session
            GameSession gameSession = vendorService.getGameSession(dto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession, sign, body);

            // Get transaction type to process accordingly
            Map<String, RoundPayoutTransactionDto> txnMap = vendorService.getTransactions(dto);

            // 1. bet + win (unsettled, settled)
            // 2. only bet (unsettled)
            // 3. only win (unsettled, settled)
            // 4. cancel bet (on unsettled bets)

            if (vendorService.isBetAndWin(txnMap)) {
                // processBetResult
                roundPayoutVo = betService.doBetAndWin(httpRequestLog, traceId, gameSession, dto, txnMap);

            } else if (vendorService.isBetTransactionOnly(txnMap)) {
                // processBet
                roundPayoutVo = betService.doBet(httpRequestLog, traceId, gameSession, dto, txnMap, body);

            } else if (vendorService.isWinTransactionOnly(txnMap)) {
                // processBetResult
                roundPayoutVo = betService.doWin(httpRequestLog, traceId, gameSession, dto, txnMap);

            } else if (vendorService.isCancelBet(txnMap)) {
                // processRollback
                roundPayoutVo = cancelBetService.cancelBet(httpRequestLog, traceId, gameSession, dto);
            } else {
                // when all fails due to unknown reason log and throw exception
                log.warn(traceId + ": Unhandled transaction type -> " + body);
                throw new Exception();
            }

        } catch (InvalidPlayerException | DisabledVendorLineException |
                 DisabledAgentPlayerException userNotFoundException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);

        } catch (AuthenticationException | InvalidVendorLineException |
                 CredentialNotFoundException tokenNotFoundOrInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_TOKEN_NOT_FOUND_OR_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_UNAUTHORIZED);

        } catch (GameNotSupportedException gameNotSupportedException) {
            roundPayoutErrorVo.setCode(ResponseCodes.GAME_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);

        } catch (DisabledGameException disabledGameException) {
            roundPayoutErrorVo.setCode(ResponseCodes.GAME_NOT_AVAILABLE);
            roundPayoutVo.setStatus(HttpStatus.SC_FORBIDDEN);

        } catch (DateTimeParseException | CurrencyNotSupportedException | JsonProcessingException |
                 NullPointerException | IllegalArgumentException parameterInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (InvalidRequestException invalidRequestException) {
            if(invalidRequestException.getMessage().equals(Exceptions.INVALID_AMOUNT)) {
                roundPayoutErrorVo.setCode(ResponseCodes.TRANSACTION_INVALID);
                roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            } else {
                roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
                roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception exception) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            if (roundPayoutVo.getStatus() != HttpStatus.SC_OK && roundPayoutVo.getError() == null) {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
            httpService.end(httpRequestLog, roundPayoutVo);
        }

        return new ResponseEntity<>(roundPayoutVo, HttpStatusCode.valueOf(roundPayoutVo.getStatus()));
    }

    private void doValidation(RoundPayoutDto dto, String token) throws InvalidRequestException {
        Optional.ofNullable(token).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RoundPayoutDto dto, GameSession gameSession, String token, String body)
            throws InvalidPlayerException, InvalidRequestException, GameNotSupportedException,
            AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException, CurrencyNotSupportedException, InvalidVendorLineException,
            CredentialNotFoundException, JsonProcessingException {

        // Convert object to Map for signature check
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> bodyObj = mapper.readValue(body, Map.class);

        // Get signature key
        String signatureKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE_KEY);

        // Verify signature
        VendorService.validateSignature(token, bodyObj, signatureKey);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT);

        // General validation
        for (RoundPayoutTransactionDto obj : dto.getTransactionList()) {
            formatter.parse(obj.getTimestamp());
            ValidationUtils.validateRequest(obj);
            switch (obj.getType()) {
                case TransactionType.BET:
                    if (obj.getAmount().compareTo(BigDecimal.ZERO) > 0)
                        throw new InvalidRequestException(Exceptions.INVALID_AMOUNT);
                    break;
                case TransactionType.WIN:
                case TransactionType.CANCEL_BET:
                    if (obj.getAmount().compareTo(BigDecimal.ZERO) < 0)
                        throw new InvalidRequestException(Exceptions.INVALID_AMOUNT);
                    break;
                default:
                    throw new InvalidRequestException();
            }
        }

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUserId());

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
    }

}