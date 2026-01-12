package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.endround;

import com.nextgen.gameaggregator.core.engine.game.round.GameRoundService;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Component
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@RequiredArgsConstructor
@Slf4j
public class EndRoundAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final GameRoundService gameRoundService;
    private final VendorService vendorService;

    public ResponseVo endRoundRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        EndRoundVo responseVo = new EndRoundVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            EndRoundDto dto = HttpService.convertQueryStringToDto(httpRequestLog, EndRoundDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession;
            try {
                gameSession = gameSessionService.verifyToken(dto.getToken());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameId(), gameSession);
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            BetResultContextHolder.initialise()
                    .configure(config -> config.setSettleType(SettleType.ROUND));
            BetResultContext betResultContext = BetResultContextHolder.getBetResultContext();
            betResultContext.setRoundEnded(true);

            // insert to kafka process round ended
            gameRoundService.publishRoundEnded(dto.getRoundId(), gameSession);

            responseVo.setCash(BigDecimal.ZERO);
            responseVo.setBonus(BigDecimal.ZERO);
        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(EndRoundDto dto) throws InvalidRequestException, InvalidPlayerException {

        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }
}
