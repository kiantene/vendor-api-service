package com.nextgen.gameaggregator.vendor.aglive.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.aglive.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetService {

    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;

    @Autowired
    public BetService(VendorService vendorService, HttpService httpService, GameSessionService gameSessionService,
                      WalletService walletService, VendorLineService vendorLineService, ValidationService validationService,
                      AgentPlayerService agentPlayerService, VendorGameService vendorGameService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();

        try {
            CommonBetDto commonBetDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonBetDto.class);
            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonBetDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonBetDto.getBetDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonBetDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BetEvent betEvent = walletService.processBet(traceId, gameSession, commonBetDto, httpRequestLog.getRequestBody(), httpRequestLog);

            // set getbalanceVo
            vo.setSuccessResponse(betEvent.getLastBalance());

        } catch (InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 TransactionStillProcessingException |
                 JsonProcessingException |
                 DisabledVendorLineException e) {
            //set Vo
            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (InsufficientBalanceException e) {

            vo.setErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);

        } catch (BetResultIdempotentViolationException e) {

            vo.setSuccessResponse(vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog));
            httpService.logError(httpRequestLog, e);
            
        } catch (AuthenticationException | InvalidPlayerException | CurrencyNotSupportedException |
                 VendorCurrencyNotSupportException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_SESSION);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {

            vo.setErrorResponse(ResponseCodes.ERROR);
            httpService.logError(httpRequestLog, e);
        }
        return vo;
    }

    private void doValidation(CommonBetDto commonBetDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonBetDto);

        ValidationUtils.validateRequest(commonBetDto.getBetDto());
    }

    private void doVerification(CommonBetDto commonBetDto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException,
            CredentialNotFoundException, InvalidVendorLineException, CurrencyNotSupportedException {

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String param = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.ACCOUNT);
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Validate Username
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), commonBetDto.getBetDto().getPlayName(),
                InvalidPlayerException::new);
        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonBetDto.getBetDto().getCurrency(), CurrencyNotSupportedException::new);
    }
}
