package com.nextgen.gameaggregator.vendor.cpgame.api.credit;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
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
public class CreditAction {

    private final VendorService vendorService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    private final RequestIdempotentLogService requestIdempotentLogService;


    @Autowired
    public CreditAction(VendorService vendorService, HttpService httpService,
                        VendorLineService vendorLineService, GameSessionService gameSessionService,
                        WalletService walletService, VendorPlayerService vendorPlayerService,
                        RequestIdempotentLogService requestIdempotentLogService) {

        this.vendorService = vendorService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.SETTLED)
    public ResponseVo credit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo vo = new ResponseVo();

        DataVo dataVo = new DataVo();
        CreditDto creditDto = new CreditDto();
        boolean isRequestExists = false;
        VendorPlayer vendorPlayer = new VendorPlayer();
        try {
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), "UTF-8");

            creditDto = HttpService.convertQueryStringToDto(body, CreditDto.class);
            creditDto.setMessageDto(creditDto.getMessage());

            this.doValidation(creditDto);

            Long vendorPlayerId = (long) creditDto.getMessageDto().getSubUid();
            vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);

            //check for idempotent request
            if (requestIdempotentLogService.checkExists(creditDto, vendorPlayer.getUsername()) == null) {
                requestIdempotentLogService.create(creditDto, vendorPlayer.getUsername());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // using vendorPlayerId to find gameSession details
            GameSession gameSession;
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(creditDto.getGameId(), gameSession);
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(vendorPlayer.getUsername()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, creditDto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession.getVendorLineId(), body);

            ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false);

            // Set it as unsettle status even the bet request will show is end round
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);
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

        } catch (InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.PLAYER_NOT_EXIST);

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.RETRY_LATER);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INVALID_TRANSACTION);

        } catch (BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.BET_NOT_FOUND);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.UNKNOWN_ERROR);

        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(creditDto, vendorPlayer.getUsername());
            }
            httpService.end(httpRequestLog, vo);
        }
        return vo;
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessageDto());
        ValidationUtils.validateRequest(dto.getMessageDto().getBetInfo());
        if (dto.getMessageDto().getBetInfo().getWinAmount() == null) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(CreditDto dto, Integer vendorLineId, String oriRequest) throws
            CredentialNotFoundException, InvalidSignatureException {

        String appId = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.app_id);
        ValidationUtils.isEquals(appId, dto.getAppid(), CredentialNotFoundException::new);

        // Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.secret_key);
        VendorService.verifyHash(oriRequest, dto.getToken(), secretKey);

    }


}
