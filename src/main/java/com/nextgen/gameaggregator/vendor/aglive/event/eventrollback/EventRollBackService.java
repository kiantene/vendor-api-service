package com.nextgen.gameaggregator.vendor.aglive.event.eventrollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aglive.event.constant.EventCode;
import com.nextgen.gameaggregator.vendor.aglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.aglive.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class EventRollBackService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;

    @Autowired
    public EventRollBackService(GameSessionService gameSessionService,
                                VendorLineService vendorLineService,
                                WalletService walletService,
                                HttpService httpService,
                                AgentPlayerService agentPlayerService,
                                VendorGameService vendorGameService,
                                VendorService vendorService) {
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
    }

    public CommonVo rollback(HttpRequestLog httpRequestLog, String traceId) {

        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;

        try {
            CommonEventRollBackDto commonEventRollBackDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonEventRollBackDto.class);
            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonEventRollBackDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonEventRollBackDto.getEventRollBackDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonEventRollBackDto, gameSession);

            //check if is dealer then can roll back
            if (EventCode.DEALER_TIPS.getCode().equals(commonEventRollBackDto.getEventRollBackDto().getEventID())) {
                vendorService.setRejectSettleAfterRollback(false);
            }
            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, commonEventRollBackDto, gameSession, vendorService, httpRequestLog);

            vo.setSuccessResponse(balance);

        } catch (DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 RecordNotFoundException |
                 JsonProcessingException |
                 BetNotFoundException |
                 BetRefundIdempotentViolationException |
                 TransactionStillProcessingException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException |
                 InvalidPlayerException |
                 VendorCurrencyNotSupportException |
                 GameNotSupportedException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_SESSION);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {

            vo.setSuccessResponse(vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog));
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {

            // set errorVo
            vo.setErrorResponse(ResponseCodes.ERROR);
            httpService.logError(httpRequestLog, e);
        }
        return vo;
    }

    private void doValidation(CommonEventRollBackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getEventRollBackDto());
        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRollbackId());

    }

    private void doVerification(CommonEventRollBackDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            AuthenticationException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            GameNotSupportedException {
        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String param = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.ACCOUNT);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        if (EventCode.isInvalidEventID(dto.getEventRollBackDto().getEventID())) {
            throw new GameNotSupportedException("Invalid eventID: " + dto.getEventRollBackDto().getEventID());
        }
        if (EventCode.isNotDealerTips(dto.getEventRollBackDto().getEventID())) {
            throw new DisabledGameException("EventID not supported." + dto.getEventRollBackDto().getEventID());
        }

        // Validate Username
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), dto.getEventRollBackDto().getPlayName(),
                InvalidPlayerException::new);

    }
}

