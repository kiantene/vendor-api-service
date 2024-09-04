package com.nextgen.gameaggregator.vendor.hacksaw.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksaw.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CreditService {

    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    @Autowired
    public CreditService(GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         WalletService walletService,
                         HttpService httpService,
                         VendorService vendorService,
                         RequestIdempotentLogService requestIdempotentLogService) {

        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public ResponseVo credit(HttpRequestLog httpRequestLog, String traceId) {
        ResponseVo vo = new ResponseVo();

        BigDecimal balance = null;
        GameSession gameSession = new GameSession();
        boolean isRequestExists = false;
        CreditDto creditDto = new CreditDto();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // request idempotent checking.
            if (requestIdempotentLogService.checkExists(creditDto, creditDto.getExternalPlayerId()) == null) {
                requestIdempotentLogService.create(creditDto, creditDto.getExternalPlayerId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Verify session token
            try {
                gameSession = gameSessionService.verifyToken(creditDto.getExternalSessionId());
            } catch (AuthenticationException authenticationException) {
                String vendorGameCode = "hsg_hsg_" + creditDto.getGameId();
                gameSession = gameSessionService.generateNewSessionToken(creditDto.getExternalPlayerId());
                gameSessionService.updateByVendorGameCode(gameSession, vendorGameCode);
                gameSessionService.updateByVendorCurrencyCode(gameSession, creditDto.getCurrency());
                gameSession.setToken(creditDto.getExternalSessionId());
                gameSession.setVendorToken(creditDto.getExternalSessionId());
            }

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession);

            ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false, creditDto.getBetStatus());
            balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            // set vo
            vo.setAccountBalance(balance.longValue());
            vo.setExternalTransactionId(creditDto.getExternalTransactionId());

        } catch (InvalidPlayerException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_USER_OR_TOKEN_EXPIRED);
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

        } catch (BetResultIdempotentViolationException e) {
            balance = getCurrentBalance(gameSession, httpRequestLog);
            vo.setAccountBalance(balance.longValue());
            vo.setResponseCodes(ResponseCodes.SUCCESS);
            httpService.logError(httpRequestLog, e);

        } catch (BetNotFoundException | InvalidOperatorResponseException | InvalidAgentApiCredentialException |
                 TransactionStillProcessingException e) {
            if (e instanceof BetNotFoundException) {
                balance = getCurrentBalance(gameSession, httpRequestLog);
                vo.setAccountBalance(balance.longValue());
            }
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);

        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(creditDto, creditDto.getExternalPlayerId());
            }
        }

        return vo;
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // check round id is null or not
        Optional.ofNullable(dto.getRoundId()).orElseThrow(InvalidRequestException::new);
        // check betTransactionId is null or not
        Optional.ofNullable(dto.getBetTransactionId()).orElseThrow(InvalidRequestException::new);
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession)
            throws InvalidPlayerException, CredentialNotFoundException,
            CurrencyNotSupportedException {

        //Verify received secret is same with credential
        String secret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        ValidationUtils.isEquals(secret, dto.getSecret(), CredentialNotFoundException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getExternalPlayerId(), InvalidPlayerException::new);
    }

    private BigDecimal getCurrentBalance(GameSession gameSession, HttpRequestLog httpRequestLog) {

        BigDecimal balance = BigDecimal.ZERO;
        HttpRequestLog newHttpRequestLog = new HttpRequestLog();

        try {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setSkipNullEnabled(true);
            modelMapper.map(httpRequestLog, newHttpRequestLog);
            newHttpRequestLog.setId(UUID.randomUUID().toString());
            balance = walletService.getBalance(newHttpRequestLog.getId(), gameSession, newHttpRequestLog);

        } catch (Exception ignored) {
            // do nothing's
        } finally {
            httpService.end(newHttpRequestLog, new ResponseVo());
        }

        return balance;
    }
}
