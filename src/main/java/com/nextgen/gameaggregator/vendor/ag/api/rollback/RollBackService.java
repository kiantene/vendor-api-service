package com.nextgen.gameaggregator.vendor.ag.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ag.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ag.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ag.service.VendorService;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RollBackService {

    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;

    @Autowired
    public RollBackService(GameSessionService gameSessionService,
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
            CommonRollBackDto commonRollBackDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonRollBackDto.class);
            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonRollBackDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonRollBackDto.getRollBackDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonRollBackDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, commonRollBackDto, gameSession, vendorService, httpRequestLog);

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
                 CurrencyNotSupportedException |
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

    private void doValidation(CommonRollBackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getRollBackDto());
        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRollbackId());
        
    }

    private void doVerification(CommonRollBackDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            AuthenticationException,
            InvalidPlayerException,
            GameNotSupportedException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException {
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

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getRollBackDto().getGameId(), GameNotSupportedException::new);

        // Validate Username
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), dto.getRollBackDto().getPlayName(),
                InvalidPlayerException::new);

        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getRollBackDto().getCurrency(),
                CurrencyNotSupportedException::new);


    }
}
