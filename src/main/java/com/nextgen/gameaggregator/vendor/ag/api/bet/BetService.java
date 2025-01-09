package com.nextgen.gameaggregator.vendor.ag.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ag.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ag.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ag.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ag.service.VendorService;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class BetService {

    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final ValidationService validationService;

    @Autowired
    public BetService(ValidationService validationService,
                      HttpService httpService,
                      VendorService vendorService,
                      WalletService walletService,
                      GameSessionService gameSessionService,
                      VendorGameService vendorGameService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService) {
        this.validationService = validationService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorGameService = vendorGameService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();

        try {
            CommonDto commonDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonDto.getRecordDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BetEvent betEvent = walletService.processBet(traceId, gameSession, commonDto,
                    httpRequestLog.getRequestBody(), httpRequestLog);
            // set getbalanceVo
            vo.setSuccessResponse(betEvent.getLastBalance());

        } catch (InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 JsonProcessingException |
                 TransactionStillProcessingException |
                 InsufficientBalanceException |
                 DisabledVendorLineException e) {
            //set Vo
            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException |
                 InvalidPlayerException |
                 VendorCurrencyNotSupportException |
                 CurrencyNotSupportedException |
                 GameNotSupportedException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_SESSION);
            httpService.logError(httpRequestLog, e);

        } catch (BetResultIdempotentViolationException e) {

            vo.setSuccessResponse(vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog));
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setErrorResponse(ResponseCodes.ERROR);
            httpService.logError(httpRequestLog, e);

        }
        return vo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRecordDto());
    }

    private void doVerification(CommonDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            AuthenticationException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

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
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), dto.getRecordDto().getPlayName(),
                InvalidPlayerException::new);
        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());
        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getRecordDto().getCurrency(),
                CurrencyNotSupportedException::new);
    }

}
