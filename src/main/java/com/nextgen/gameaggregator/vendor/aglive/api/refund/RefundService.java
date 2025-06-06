package com.nextgen.gameaggregator.vendor.aglive.api.refund;

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
import com.nextgen.gameaggregator.vendor.aglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.aglive.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RefundService {

    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;

    @Autowired
    public RefundService(GameSessionService gameSessionService, VendorLineService vendorLineService, WalletService walletService,
                         HttpService httpService, AgentPlayerService agentPlayerService, VendorGameService vendorGameService,
                         VendorService vendorService) {
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
    }

    public CommonVo refund(HttpRequestLog httpRequestLog, String traceId) {

        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;

        try {
            CommonRefundDto commonRefundDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonRefundDto.class);
            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonRefundDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonRefundDto.getRefundDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonRefundDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, commonRefundDto, gameSession, vendorService, httpRequestLog);

            vo.setSuccessResponse(balance);

        } catch (DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 RecordNotFoundException |
                 JsonProcessingException |
                 BetRefundIdempotentViolationException |
                 TransactionStillProcessingException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (BetResultIdempotentViolationException e) {

            vo.setSuccessResponse(vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog));
            httpService.logError(httpRequestLog, e);

        } catch (BetNotFoundException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_TRANSACTION);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException | InvalidPlayerException | CurrencyNotSupportedException |
                 VendorCurrencyNotSupportException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_SESSION);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {

            // set errorVo
            vo.setErrorResponse(ResponseCodes.ERROR);
            httpService.logError(httpRequestLog, e);
        }
        return vo;
    }

    private void doValidation(CommonRefundDto commonRefundDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonRefundDto);

        //check object inside the dto
        ValidationUtils.validateRequest(commonRefundDto.getRefundDto());


    }

    private void doVerification(CommonRefundDto commonRefundDto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException, AuthenticationException, InvalidPlayerException, CredentialNotFoundException,
            InvalidVendorLineException, CurrencyNotSupportedException {
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

        // Validate Username
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), commonRefundDto.getRefundDto().getPlayName(),
                InvalidPlayerException::new);

        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonRefundDto.getRefundDto().getCurrency(), CurrencyNotSupportedException::new);
    }
}
