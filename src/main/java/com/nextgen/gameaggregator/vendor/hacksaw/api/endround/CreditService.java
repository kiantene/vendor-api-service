package com.nextgen.gameaggregator.vendor.hacksaw.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksaw.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class CreditService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;

    public ResponseVo credit(HttpRequestLog httpRequestLog, String traceId) {
        ResponseVo vo = new ResponseVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // check round id is null or not
            creditDto.checkRoundId();

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(creditDto.getExternalSessionId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession);

            ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false, creditDto.getBetStatus());
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            // set vo
            vo.setAccountBalance(balance.longValue());
            vo.setExternalTransactionId(creditDto.getExternalTransactionId());

        } catch (AuthenticationException | InvalidPlayerException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_USER_OR_TOKEN_EXPIRED);
            httpService.logError(httpRequestLog, e);

        } catch (DisabledAgentPlayerException e) {
            vo.setResponseCodes(ResponseCodes.ACCOUNT_LOCKED);
            httpService.logError(httpRequestLog, e);

        } catch (JsonProcessingException | InvalidRequestException | CredentialNotFoundException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_ACTION);
            httpService.logError(httpRequestLog, e);

        } catch (InsufficientBalanceException e) {
            vo.setResponseCodes(ResponseCodes.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);

        } catch (CurrencyNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_CURRENCY);
            httpService.logError(httpRequestLog, e);

        } catch (GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
            vo.setStatusMessage("Unknown Game ID");
            httpService.logError(httpRequestLog, e);

        } catch (BetNotFoundException | BetResultIdempotentViolationException e) {
            vo.setResponseCodes(ResponseCodes.SUCCESS);
            httpService.logError(httpRequestLog, e);

        } catch (DisabledVendorLineException | DisabledGameException | InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException | TransactionStillProcessingException e) {
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
        }

        return vo;
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException, CredentialNotFoundException, CurrencyNotSupportedException, GameNotSupportedException {

        //Verify received secret is same with credential
        String secret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        ValidationUtils.isEquals(secret, dto.getSecret(), CredentialNotFoundException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify valid game id
        vendorService.verifyVendorGameCode(gameSession, dto.getGameId().toString());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getExternalPlayerId(), InvalidPlayerException::new);
    }
}
