package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.WalletService;
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

import java.lang.reflect.InvocationTargetException;
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

    @PostMapping(path = EndPoints.RELEASE)
    public String release(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ReleaseVo releaseVo = new ReleaseVo();
        XmlMapper xmlMapper = new XmlMapper();
        String releaseVoXml = "";
        GameSession gameSession = null;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into commonDto
            ReleaseDto releaseDto = xmlMapper.readValue(body, ReleaseDto.class);
            log.info("Playngo Release body: " + body);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(releaseDto);

            // Get game session or verify Token
            gameSession = vendorService.getGameSession(releaseDto);

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
                 InvalidRequestException |
                 NoSuchMethodException |
                 InvocationTargetException |
                 IllegalAccessException internalErrorException) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (VendorCurrencyNotSupportException | CurrencyNotSupportedException invalidCurrencyException) {
            releaseVo.setStatusCode(ResponseCodes.INVALIDCURRENCY);

        } catch (AuthenticationException authenticationException) {
            releaseVo.setStatusCode(ResponseCodes.SESSIONEXPIRED);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            releaseVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);
            vendorService.setCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, releaseVo);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            releaseVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            releaseVo.setStatusCode(ResponseCodes.OK);
            releaseVo.setReal(betResultIdempotentViolationException.getBalance());

        } catch (BetNotFoundException betNotFoundException) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if(invalidOperatorResponseException.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                releaseVo.setStatusCode(ResponseCodes.NOTENOUGHMONEY);
                vendorService.setCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, releaseVo);

            } else {
                releaseVo.setStatusCode(ResponseCodes.MAXCONCURRENTCALLS);
                httpService.logError(httpRequestLog, invalidOperatorResponseException);

            }

        } catch (Exception exception) {
            releaseVo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        } finally {
            try {
                releaseVoXml = xmlMapper.writeValueAsString(releaseVo);

            } catch (JsonProcessingException e) {
                releaseVo.setStatusCode(ResponseCodes.INTERNAL);

            }

            releaseVo.setResponseXMLFormat(releaseVoXml);
            httpService.end(httpRequestLog, releaseVo);

        }

        return releaseVoXml;
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
            InvalidOperatorResponseException {

        if (releaseDto.getState().equals("1") && releaseDto.getRoundId().equals("0") && releaseDto.getReal().compareTo(BigDecimal.ZERO) == 0) {
            return walletService.getBalance(traceId, gameSession, httpRequestLog);

        } else {
            // Check if bet record has been settled before
            this.verifySettledBet(releaseDto, gameSession);

            // Get result type: (WIN / LOSE / END) or (BET_WIN / BET_LOSE)
            ResultType resultType = this.getResultType(releaseDto, gameSession);

            // Process Bet Result
            return walletService.processBetResult(traceId, gameSession, releaseDto, resultType, vendorService, httpRequestLog);

        }

    }

    private ResultType getResultType(ReleaseDto dto, GameSession gameSession) {
        Boolean isBet = this.verifyUnsettleBet(dto, gameSession);
        ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), isBet);

        return resultType;
    }

    private boolean verifyUnsettleBet(ReleaseDto dto, GameSession gameSession) {
        List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

        if (unsettledBetList.isEmpty()) {
            return true;
        }

        return false;
    }

    private void verifySettledBet(ReleaseDto dto, GameSession gameSession) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getRoundId());

        if (settledBetList.size() > 0) {
            throw new BetResultIdempotentViolationException();
        }
    }

}
