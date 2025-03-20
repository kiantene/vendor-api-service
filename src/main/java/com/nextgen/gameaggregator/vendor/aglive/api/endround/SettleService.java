package com.nextgen.gameaggregator.vendor.aglive.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
public class SettleService {

    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;

    @Autowired
    public SettleService(GameSessionService gameSessionService, WalletService walletService, AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService, VendorService vendorService, VendorLineService vendorLineService,
                         HttpService httpService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.httpService = httpService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, String traceId) {

        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;

        try {
            CommonSettleDto commonSettleDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonSettleDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonSettleDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonSettleDto.getSettleDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonSettleDto, gameSession);

            ResultType resultType = vendorService.checkGameType(commonSettleDto.getSettleDto().getGameType(),
                    commonSettleDto.getWinAmount(),
                    commonSettleDto.getSettleDto().getFinish());
            
            balance = walletService.processBetResult(traceId, gameSession, commonSettleDto, resultType, vendorService, httpRequestLog);

            // set vo
            vo.setSuccessResponse(balance);

        } catch (InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException |
                 InvalidRequestException |
                 TransactionStillProcessingException |
                 JsonProcessingException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (BetNotFoundException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_TRANSACTION);
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

    private void doValidation(CommonSettleDto commonSettleDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonSettleDto);
        //check object inside the dto
        ValidationUtils.validateRequest(commonSettleDto.getSettleDto());
    }

    private void doVerification(CommonSettleDto commonSettleDto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException, InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidVendorLineException,
            CurrencyNotSupportedException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String param = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.ACCOUNT);
        // Validate Username
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), commonSettleDto.getSettleDto().getPlayName(),
                InvalidPlayerException::new);
        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonSettleDto.getSettleDto().getCurrency(), CurrencyNotSupportedException::new);
    }
}
