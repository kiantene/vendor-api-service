package com.nextgen.gameaggregator.vendor.db.api.balance;

import com.fasterxml.jackson.core.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.db.constant.Credentials;
import com.nextgen.gameaggregator.vendor.db.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.db.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.db.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.db.service.VendorService;
import com.nextgen.gameaggregator.vendor.db.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    @Autowired
    public BalanceAction(
            HttpService httpService,
            VendorLineService vendorLineService,
            AgentPlayerService agentPlayerService,
            VendorGameService vendorGameService,
            GameSessionService gameSessionService,
            WalletService walletService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        BalanceDataVo balanceDataVo = new BalanceDataVo();

        ResponseVo vo = new ResponseVo();
        CommonDto commonDto;
        try {
            //get body and queryString from vendor request
            String body = httpRequestLog.getRequestBody();
            String queryString = request.getQueryString();

            //convert queryString to dto
            commonDto = HttpService.convertQueryStringToDto(queryString, CommonDto.class);

            //do validation
            ValidationUtils.validateRequest(commonDto);

            String jsonBody = VendorService.decryptToJsonBody(commonDto, vendorLineService, body);
            BalanceDto balanceDto = HttpService.convertJsonToDto(jsonBody, BalanceDto.class);
            httpRequestLog.setRequestBody(VendorService.
                    getEncryptJsonQueryStringBody(body, jsonBody, queryString));

            //validate request param
            this.doValidation(balanceDto);

            // using vendorPlayerId to find gameSession details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getMemberId());

            //Verification
            this.doVerification(balanceDto, gameSession, commonDto);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            vo.setResponseCode(ResponseCodes.SUCCESS);
            balanceDataVo.setBalance(balance.toBigInteger());
            vo.setData(balanceDataVo);

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.PLAYER_NOT_EXIST_1);

        } catch (DisabledGameException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.INVALID_GAME_ID);

        } catch (CredentialNotFoundException | JsonParseException |
                 UnexpectedTypeException | InvalidPlayerException | InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.INVALID_PARAMETER);

        } catch (InvalidSignatureException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setResponseCode(ResponseCodes.INVALID_SIGNATURE_1);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR_1);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(BalanceDto balanceDto) throws InvalidRequestException {

        // Validation with custom exception
        ValidationUtils.validateRequest(balanceDto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession,
                                CommonDto commonDto) throws
            InvalidPlayerException, DisabledVendorLineException, CredentialNotFoundException,
            DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        VendorService.verifyHash(commonDto.getAgent(), commonDto.getTimestamp(), secretKey, commonDto.getSign());
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getMemberId(), InvalidPlayerException::new);
    }

}
