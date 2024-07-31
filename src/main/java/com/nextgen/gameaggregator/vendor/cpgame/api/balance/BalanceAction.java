package com.nextgen.gameaggregator.vendor.cpgame.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cpgame.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cpgame.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cpgame.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cpgame.service.VendorService;
import com.nextgen.gameaggregator.vendor.cpgame.vo.DataVo;
import com.nextgen.gameaggregator.vendor.cpgame.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorPlayerService vendorPlayerService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    @Autowired
    public BalanceAction(HttpService httpService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         VendorPlayerService vendorPlayerService,
                         GameSessionService gameSessionService,
                         WalletService walletService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorPlayerService = vendorPlayerService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo vo = new ResponseVo();

        DataVo dataVo = new DataVo();
        BalanceDto balanceDto = null;

        try {
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), StandardCharsets.UTF_8);

            balanceDto = HttpService.convertQueryStringToDto(body, BalanceDto.class);

            // convert message value to json object
            balanceDto.convertStringToJsonObject(balanceDto.getMessage());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            Integer vendorPlayerId = balanceDto.getMessageDto().getSubUid();
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);

            // using vendorPlayerId to find gameSession details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession, body);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            dataVo.setBalance(balance);
            dataVo.setCurrency(gameSession.getVendorCurrencyCode());

            vo.setCodeMsg(ResponseCodes.SUCCESS);
            vo.setData(dataVo);

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INVALID_REQUEST);

        } catch (InvalidSignatureException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.SIGNATURE_ERROR);

        } catch (CredentialNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.APP_ID_ERROR);

        } catch (DisabledGameException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.GAME_ID_ERROR);

        } catch (AuthenticationException | InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.PLAYER_NOT_EXIST);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.UNKNOWN_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);
        }
        return vo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessageDto());
    }

    private void doVerification(BalanceDto dto, GameSession gameSession, String oriRequest)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException,
            CredentialNotFoundException, InvalidSignatureException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        String appId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.app_id);
        ValidationUtils.isEquals(appId, dto.getAppid(), CredentialNotFoundException::new);

        // Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret_key);
        VendorService.verifyHash(oriRequest, dto.getToken(), secretKey);

    }
}
