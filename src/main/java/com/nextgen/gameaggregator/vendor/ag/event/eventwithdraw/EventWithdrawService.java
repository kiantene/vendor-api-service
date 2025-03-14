package com.nextgen.gameaggregator.vendor.ag.event.eventwithdraw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ag.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ag.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ag.event.constant.EventCode;
import com.nextgen.gameaggregator.vendor.ag.event.eventdto.CommonEventDto;
import com.nextgen.gameaggregator.vendor.ag.service.VendorService;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class EventWithdrawService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final ValidationService validationService;

    @Autowired
    public EventWithdrawService(ValidationService validationService,
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

    public CommonVo withdraw(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;

        try {
            CommonEventDto commonEventDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonEventDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonEventDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonEventDto.getEventDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonEventDto, gameSession);

            balance = walletService.processBetResult(traceId, gameSession, commonEventDto, ResultType.BET_LOSE, vendorService, httpRequestLog);

            // set getbalanceVo
            vo.setSuccessResponse(balance);

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
                 GameNotSupportedException |
                 VendorCurrencyNotSupportException |
                 CurrencyNotSupportedException e) {

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

    private void doValidation(CommonEventDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        //check object inside the dto
        ValidationUtils.validateRequest(dto.getEventDto());
    }

    private void doVerification(CommonEventDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            AuthenticationException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException {

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
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), dto.getEventDto().getPlayName(),
                InvalidPlayerException::new);
        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        if (EventCode.isInvalidEventID(dto.getEventDto().getEventID())) {
            throw new GameNotSupportedException("Invalid eventID: " + dto.getEventDto().getEventID());
        }
        if (EventCode.isNotDealerTips(dto.getEventDto().getEventID())) {
            throw new DisabledGameException("EventID not supported." + dto.getEventDto().getEventID());
        }

        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getEventDto().getCurrency(),
                CurrencyNotSupportedException::new);
    }

}
