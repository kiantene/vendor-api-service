package com.nextgen.gameaggregator.vendor.poker365.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.poker365.service.VendorService;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class CancelService {
    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    Integer vendorPlayerId;

    @Autowired
    public CancelService(HttpService httpService, GameSessionService gameSessionService,
                         AgentPlayerService agentPlayerService, VendorLineService vendorLineService,
                         VendorService vendorService,
                         WalletService walletService, VendorPlayerService vendorPlayerService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public CommonVo cancel(HttpRequestLog httpRequestLog, String traceId) throws JsonProcessingException {
        CommonVo commonVo = new CommonVo();
        BigDecimal balance;
        try {

            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = VendorService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);
            String formatedMessageDto = commonDto.getMessage();
            MessageDto messageDto = HttpService.convertJsonToDto(formatedMessageDto, MessageDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(commonDto, messageDto);

            this.vendorPlayerId = Integer.valueOf(messageDto.getUserId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());


            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, messageDto, gameSession);

            if ("26184741".equals(String.valueOf(gameSession.getVendorPlayerId()))) {
                Thread.sleep(2000);
            }
            balance = walletService.processRollback(traceId, messageDto, gameSession, vendorService, httpRequestLog);

            commonVo.setBalance(balance);
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);

        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException e) {
            commonVo.setStatus(ResponseCodes.NO_DATA.status);
            commonVo.setMsg(ResponseCodes.NO_DATA.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {
            commonVo.setStatus(ResponseCodes.USERNAME_INVALID.status);
            commonVo.setMsg(ResponseCodes.USERNAME_INVALID.message);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            commonVo.setStatus(ResponseCodes.NOT_AUTHORIZED.status);
            commonVo.setMsg(ResponseCodes.NOT_AUTHORIZED.message);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            commonVo.setStatus(ResponseCodes.INVALID_PARAMETERS.status);
            commonVo.setMsg(ResponseCodes.INVALID_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CommonDto commonDto, MessageDto messageDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
        ValidationUtils.validateRequest(messageDto);
    }

    private void doVerification(CommonDto commonDto, MessageDto messageDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, InvalidVendorLineException, InvalidPlayerException, CredentialNotFoundException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(cert, commonDto.getKey(), AuthenticationException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), messageDto.getUserId(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
