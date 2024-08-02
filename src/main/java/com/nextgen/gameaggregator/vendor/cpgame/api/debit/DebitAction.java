package com.nextgen.gameaggregator.vendor.cpgame.api.debit;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class DebitAction {
    private final HttpService httpService;

    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;

    @Autowired
    public DebitAction(HttpService httpService,
                       VendorLineService vendorLineService,
                       ValidationService validationService,
                       GameSessionService gameSessionService,
                       WalletService walletService,
                       VendorPlayerService vendorPlayerService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorPlayerService = vendorPlayerService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.UNSETTLED)
    public ResponseVo debit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo vo = new ResponseVo();

        DataVo dataVo = new DataVo();

        BigDecimal balance = null;
        DebitDto debitDto = null;
        try {
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), "UTF-8");

            debitDto = HttpService.convertQueryStringToDto(body, DebitDto.class);

            debitDto.convertStringToJsonObject(debitDto.getMessage());

            this.doValidation(debitDto);

            Long vendorPlayerId = (long) debitDto.getMessageDto().getSubUid();
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);

            // using vendorPlayerId to find gameSession details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto, gameSession, body);

            // Set it as unsettle status even the bet request will show is end round
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, httpRequestLog.getRequestBody(), httpRequestLog);
            balance = betEvent.getLastBalance();

            vo.setCodeMsg(ResponseCodes.SUCCESS);

            // define time for response data to vendor
            long currentTimeMillis = System.currentTimeMillis();

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

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.SYSTEM_BUSY);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INVALID_TRANSACTION);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.UNKNOWN_ERROR);

        } finally {
            httpService.end(httpRequestLog, vo);
        }
        return vo;
    }

    private void doValidation(DebitDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessageDto());
        ValidationUtils.validateRequest(dto.getMessageDto().getBetInfo());
    }

    private void doVerification(DebitDto dto, GameSession gameSession, String oriRequest) throws
            DisabledVendorLineException, DisabledAgentPlayerException, InvalidPlayerException,
            AuthenticationException, DisabledGameException,
            CredentialNotFoundException, InvalidSignatureException {

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        String appId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.app_id);
        ValidationUtils.isEquals(appId, dto.getAppid(), CredentialNotFoundException::new);

        // Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret_key);
        VendorService.verifyHash(oriRequest, dto.getToken(), secretKey);
    }
}
