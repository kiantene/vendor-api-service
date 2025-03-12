package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playngo.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ReleaseAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping(path = EndPoints.RELEASE)
    public String release(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ReleaseVo releaseVo = new ReleaseVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = null;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            ReleaseDto releaseDto = xmlMapper.readValue(body, ReleaseDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(releaseDto);

            // Get game session by vendorPlayerUsername or verify Token
            try {
                gameSession = vendorService.getGameSessionV2(releaseDto.getExternalGameSessionId(), releaseDto.getExternalId());
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(releaseDto.getExternalId());
                gameSessionService.updateByVendorGameCode(gameSession, releaseDto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession, releaseDto);

            // Get balance if round id is 0 else process bet result
            BigDecimal balance = this.processBetResultOrGetBalance(traceId, gameSession, releaseDto, httpRequestLog);

            // Construct VO
            releaseVo.setStatusCode(ResponseCodes.OK);
            releaseVo.setReal(balance);

        } catch (InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 GameNotSupportedException |
                 CredentialNotFoundException |
                 JsonProcessingException |
                 InvalidRequestException internalErrorException) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, internalErrorException);

        } catch (VendorCurrencyNotSupportException | CurrencyNotSupportedException invalidCurrencyException) {
            releaseVo.setStatusCode(ResponseCodes.INVALIDCURRENCY);
            httpService.logError(httpRequestLog, invalidCurrencyException);

        } catch (AuthenticationException authenticationException) {
            releaseVo.setStatusCode(ResponseCodes.SESSIONEXPIRED);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            releaseVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);
            vendorService.setCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, releaseVo);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            releaseVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            releaseVo.setStatusCode(ResponseCodes.OK);
            releaseVo.setReal(betResultIdempotentViolationException.getBalance());
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                releaseVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);
                vendorService.setCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, releaseVo);

            } else {
                releaseVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            vendorService.buildResponseVo(releaseVo);
            httpService.end(httpRequestLog, releaseVo);

        }

        return releaseVo.getResponseXMLFormat();
    }

    private void doValidation(ReleaseDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, ReleaseDto releaseDto)
            throws
            InvalidPlayerException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            AuthenticationException,
            CredentialNotFoundException,
            InvalidRequestException {

        // Verify product group id
        vendorService.verifyProductId(gameSession.getVendorLineId(), releaseDto);

        // Verify vendor's access token
        vendorService.verifyAccessCode(gameSession.getVendorLineId(), releaseDto);

        // Verify bet game code
        vendorService.verifyVendorGameCode(gameSession, releaseDto.getGameId());

        // Verify Username, CurrencyCode
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), releaseDto.getExternalId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), releaseDto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private BigDecimal processBetResultOrGetBalance(String traceId, GameSession gameSession, ReleaseDto releaseDto, HttpRequestLog httpRequestLog)
            throws
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException,
            MergedBetDataIntegrityException,
            InsufficientBalanceException,
            TransactionStillProcessingException,
            BetNotFoundException,
            InvalidOperatorResponseException, InternalServerTimeoutRetryException {

        if (releaseDto.getState().equals("1") && releaseDto.getRoundId().equals("0") && releaseDto.getReal().compareTo(BigDecimal.ZERO) == 0) {
            return walletService.getBalance(traceId, gameSession, httpRequestLog);

        } else {
            // Check is unsettle bet exist
            String unsettledBetId = this.checkUnsettledBetListExist(releaseDto, gameSession);

            // Get result type: (WIN / LOSE / END) or (BET_WIN / BET_LOSE)
            // Use the first unsettled vendor bet id to do result type checking
            ResultType resultType = this.getResultType(unsettledBetId, releaseDto);

            // Process Bet Result
            return walletService.processBetResult(traceId, gameSession, releaseDto, resultType, vendorService, httpRequestLog);
        }

    }

    private ResultType getResultType(String vendorBetId, ReleaseDto dto) {
        boolean isBet = vendorBetId == null || vendorBetId.isEmpty();
        this.setVendorBetId(vendorBetId, dto);

        return vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), isBet);
    }

    private void setVendorBetId(String vendorBetId, ReleaseDto dto) {
        if (vendorBetId != null) {
            dto.setTransactionId(vendorBetId);
        }
    }

    private String checkUnsettledBetListExist(ReleaseDto dto, GameSession gameSession) {
        List<UnsettledBet> unsettledBetList;

        try {
            unsettledBetList = unsettledBetService.getByRoundIdRetry(dto.getRoundId(),
                    Integer.parseInt(dto.getGameId()), gameSession.getVendorPlayerId());
            if (unsettledBetList.isEmpty()) {
                return null;
            }
        } catch (BetNotFoundException betNotFoundException) {
            return null;
        }

        return unsettledBetList.get(0).getVendorBetId();
    }


}
