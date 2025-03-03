package com.nextgen.gameaggregator.vendor.cpgame.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {

    private final VendorService vendorService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorPlayerService vendorPlayerService;
    private final WalletService walletService;

    @Autowired
    public BetAction(VendorService vendorService, HttpService httpService,
                     VendorLineService vendorLineService, ValidationService validationService,
                     GameSessionService gameSessionService,
                     VendorPlayerService vendorPlayerService, WalletService walletService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.vendorPlayerService = vendorPlayerService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.BET)
    public ResponseVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo vo = new ResponseVo();

        DataVo dataVo = new DataVo();
        BetDto betDto = null;
        GameSession gameSession = null;
        try {
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), "UTF-8");

            betDto = HttpService.convertQueryStringToDto(body, BetDto.class);

            betDto.convertStringToJsonObject(betDto.getMessage());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            Long vendorPlayerId = (long) betDto.getMessageDto().getSubUid();
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);

            // using vendorPlayerId to find gameSession details
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession, body);

            ResultType resultType = vendorService.calculateResultType(betDto.getBetAmount(), betDto.getWinAmount(), betDto.getJackpotAmount(), true);

            // define time for response data to vendor
            long currentTimeMillis = System.currentTimeMillis();

            BigDecimal balance = walletService.processBetResult(traceId, gameSession,
                    betDto, resultType, vendorService, httpRequestLog);
            vo.setCodeMsg(ResponseCodes.SUCCESS);

            dataVo.setBalance(balance);
            dataVo.setUpdatedMs(currentTimeMillis);
            dataVo.setCurrency(gameSession.getVendorCurrencyCode());

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

        } catch (InvalidOperatorResponseException e) {
            if (e.getOperatorStatus().equals(Status.SC_INSUFFICIENT_FUNDS.code)) {

                httpService.logError(httpRequestLog, e);
                vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);
            } else {
                httpService.logError(httpRequestLog, e);
                vo.setCodeMsg(ResponseCodes.UNKNOWN_ERROR);
            }

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.SYSTEM_BUSY);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.SUCCESS);

            dataVo.setBalance(e.getBalance());
            dataVo.setUpdatedMs(System.currentTimeMillis());
            dataVo.setCurrency(gameSession.getVendorCurrencyCode());

            vo.setData(dataVo);
        } catch (BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.BET_NOT_FOUND);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.UNKNOWN_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);
        }
        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessageDto());
        ValidationUtils.validateRequest(dto.getMessageDto().getBetInfo());
    }

    private void doVerification(BetDto dto, GameSession gameSession, String oriRequest)
            throws DisabledVendorLineException, DisabledAgentPlayerException, AuthenticationException,
            DisabledGameException, InvalidPlayerException, CredentialNotFoundException,
            InvalidSignatureException {

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        String appId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.app_id);
        ValidationUtils.isEquals(appId, dto.getAppid(), CredentialNotFoundException::new);

        // Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret_key);
        VendorService.verifyHash(oriRequest, dto.getToken(), secretKey);
    }

}
