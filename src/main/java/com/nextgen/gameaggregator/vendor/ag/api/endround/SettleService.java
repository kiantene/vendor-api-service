package com.nextgen.gameaggregator.vendor.ag.api.endround;

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
import com.nextgen.gameaggregator.vendor.ag.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ag.service.VendorService;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class SettleService {

    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;

    @Autowired
    public SettleService(VendorLineService vendorLineService,
                         VendorService vendorService,
                         VendorGameService vendorGameService,
                         AgentPlayerService agentPlayerService,
                         HttpService httpService,
                         WalletService walletService,
                         GameSessionService gameSessionService) {
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.vendorGameService = vendorGameService;
        this.agentPlayerService = agentPlayerService;
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, String traceId) {

        CommonVo vo = new CommonVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;

        try {
            CommonDto commonDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonDto.getRecordDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession);

            ResultType resultType = vendorService.calculateResultType(commonDto.getBetAmount(), commonDto.getWinAmount(),
                    commonDto.getJackpotAmount(), false);

            balance = walletService.processBetResult(traceId, gameSession, commonDto, resultType, vendorService,
                    httpRequestLog);

            // set vo
            vo.setSuccessResponse(balance);

        } catch (InsufficientBalanceException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException |
                 InvalidRequestException |
                 BetNotFoundException |
                 TransactionStillProcessingException |
                 JsonProcessingException e) {

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
            CurrencyNotSupportedException,
            GameNotSupportedException {

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
