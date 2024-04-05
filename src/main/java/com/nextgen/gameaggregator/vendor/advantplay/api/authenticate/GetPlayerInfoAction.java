package com.nextgen.gameaggregator.vendor.advantplay.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.advantplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.advantplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.advantplay.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GetPlayerInfoAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.GET_PLAYER_INFO)
    public GetPlayerInfoVo getPlayerInfo(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        GetPlayerInfoVo vo = new GetPlayerInfoVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            String apHash = request.getHeader(Formats.AP_HASH);
            GetPlayerInfoDto getPlayerInfoDto = HttpService.convertJsonToDto(body, GetPlayerInfoDto.class);

            vo.setSeq(getPlayerInfoDto.getSeq());

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(getPlayerInfoDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(getPlayerInfoDto.getOpToken());

            this.doVerification(getPlayerInfoDto, gameSession, apHash, body);

            // 3. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            vo.setTimestamp(VendorService.getTimestamp());
            vo.setOpToken(gameSession.getToken());
            vo.setBrandCode(Formats.BRAND_CODE);
            vo.setSiteCode(Formats.SITE_CODE);
            vo.setPlayerId(gameSession.getVendorPlayerUsername());
            vo.setPlayerName(gameSession.getVendorPlayerUsername());
            vo.setCurrency(gameSession.getVendorCurrencyCode());
            vo.setBalance(balance);

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.TOKEN_INVALID);
            httpService.logError(httpRequestLog, e);

        } catch (GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.GAME_NOT_FOUND);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.PARAMETER_INCORRECT);
            httpService.logError(httpRequestLog, e);

        } catch (DisabledAgentPlayerException e) {
            vo.setResponseCodes(ResponseCodes.ACCOUNT_LOCKED);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.UNSPECIFIED_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto dto, GameSession gameSession, String apHash, String bodyString)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            CredentialNotFoundException,
            InvalidRequestException,
            GameNotSupportedException {

        // Verify vendor request ap-hash
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
        ValidationUtils.isEquals(vendorService.generateHash(secretKey, bodyString), apHash);

        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameCode()), GameNotSupportedException::new);

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}