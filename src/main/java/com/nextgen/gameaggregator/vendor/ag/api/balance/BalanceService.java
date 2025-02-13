package com.nextgen.gameaggregator.vendor.ag.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ag.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ag.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ag.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BalanceService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;

    @Autowired
    public BalanceService(HttpService httpService,
                          WalletService walletService,
                          GameSessionService gameSessionService,
                          VendorGameService vendorGameService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorGameService = vendorGameService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }


    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {

        XmlMapper xmlMapper = new XmlMapper();
        CommonVo vo = new CommonVo();
        GameSession gameSession;

        try {

            CommonBalanceDto commonBalanceDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CommonBalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonBalanceDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(commonBalanceDto.getBalanceDto().getSessionToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonBalanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
            xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);

            // set getbalanceVo
            vo.setSuccessResponse(balance);

        } catch (InvalidAgentApiCredentialException |
                 VendorCurrencyNotSupportException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 JsonProcessingException |
                 DisabledVendorLineException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_DATA);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException | InvalidPlayerException e) {

            vo.setErrorResponse(ResponseCodes.INVALID_SESSION);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {

            vo.setErrorResponse(ResponseCodes.ERROR);
            httpService.logError(httpRequestLog, e);
        }

        return vo;
    }

    private void doValidation(CommonBalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getBalanceDto());

    }

    private void doVerification(CommonBalanceDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidVendorLineException {

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
        ValidationUtils.isEquals(param + gameSession.getVendorPlayerUsername(), dto.getBalanceDto().getPlayName(),
                InvalidPlayerException::new);

    }

}