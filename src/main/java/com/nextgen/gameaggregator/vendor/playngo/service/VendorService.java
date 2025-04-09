package com.nextgen.gameaggregator.vendor.playngo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.playngo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playngo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playngo.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorGameCodeService vendorGameCodeService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;

    public static Long getTimestamp() {
        return Instant.now().toEpochMilli();
    }

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(), gameSession.getPlatformId(), gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }

    public void verifyAccessCode(Integer vendorLineId, CommonDto dto) throws CredentialNotFoundException, InvalidRequestException {
        String accessToken = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.ACCESS_TOKEN);
        if (!accessToken.equals(dto.getAccessToken())) {
            throw new InvalidRequestException();
        }
    }

    public void verifyProductId(Integer vendorLineId, CommonDto dto) throws CredentialNotFoundException, InvalidRequestException {
        String accessToken = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.PRODUCT_GROUP);
        if (!accessToken.equals(dto.getProductId())) {
            throw new InvalidRequestException();
        }
    }

    public void validateExternalGameSessionId(String externalGameSessionId) throws InvalidRequestException {
        if (!externalGameSessionId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new InvalidRequestException();
        }
    }

    public GameSession getGameSessionV2(String token, String vendorPlayerusername) throws AuthenticationException {
        GameSession gameSession;

        if (token == null) {
            gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(vendorPlayerusername);
            if (gameSession == null) {
                throw new AuthenticationException();
            }
        } else {
            gameSession = gameSessionService.verifyToken(token);
        }

        return gameSession;
    }

    public <T> GameSession getGameSession(T dto)
            throws
            AuthenticationException,
            InvalidRequestException,
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException {

        GameSession gameSession;

        Method getExternalGameSessionIdMethod = dto.getClass().getMethod("getExternalGameSessionId");
        String externalGameSessionId = (String) getExternalGameSessionIdMethod.invoke(dto);

        if (externalGameSessionId == null || externalGameSessionId.isEmpty()) {
            Method getExternalId = dto.getClass().getMethod("getExternalId");
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername((String) getExternalId.invoke(dto));

        } else {
            // validate extern game session id
            this.validateExternalGameSessionId(externalGameSessionId);

            // Verify session token
            gameSession = gameSessionService.verifyToken(externalGameSessionId);

        }

        return gameSession;

    }

    public void setCurrentBalanceResponseVo(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession, CommonVo vo) {

        try {
            vo.setReal(walletService.getBalance(traceId, gameSession, httpRequestLog));

        } catch (InvalidAgentApiCredentialException | InvalidOperatorResponseException internalException) {
            vo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, internalException);

        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            vo.setStatusCode(ResponseCodes.INVALIDCURRENCY);
            httpService.logError(httpRequestLog, vendorCurrencyNotSupportException);

        } catch (Exception exception) {
            vo.setStatusCode(ResponseCodes.INTERNAL);
            httpService.logError(httpRequestLog, exception);

        }

    }

    public void buildResponseVo(CommonVo vo) {
        String voXml = "";
        try {
            voXml = new XmlMapper().writeValueAsString(vo);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setStatusCode(ResponseCodes.INTERNAL);
        }
        vo.setResponseXMLFormat(voXml);
    }

}
