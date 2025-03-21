package com.nextgen.gameaggregator.vendor.cpgame.api.rollback;

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
public class RollbackAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorPlayerService vendorPlayerService;


    @Autowired
    public RollbackAction(HttpService httpService, VendorLineService vendorLineService,
                          GameSessionService gameSessionService, WalletService walletService,
                          VendorService vendorService, VendorPlayerService vendorPlayerService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    public ResponseVo rollback(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo vo = new ResponseVo();

        DataVo dataVo = new DataVo();
        RollBackDto rollBackDto = null;
        try {
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), StandardCharsets.UTF_8);
            rollBackDto = HttpService.convertQueryStringToDto(body, RollBackDto.class);
            rollBackDto.setMessageDto(rollBackDto.getMessage());

            // Validate the commonDto object
            this.doValidation(rollBackDto);

            Long vendorPlayerId = (long) rollBackDto.getMessageDto().getSubUid();
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(vendorPlayerId, null);

            // using vendorPlayerId to find gameSession details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollBackDto, gameSession, body);

            BigDecimal balance = walletService.processRollback(traceId, rollBackDto, gameSession, vendorService, httpRequestLog);
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

        } catch (AuthenticationException | InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.PLAYER_NOT_EXIST);

        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.SYSTEM_BUSY);

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
            httpService.end(httpRequestLog, vo);
        }
        return vo;
    }

    private void doValidation(RollBackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessageDto());

    }

    private void doVerification(RollBackDto dto, GameSession gameSession, String oriRequest) throws
            CredentialNotFoundException, InvalidSignatureException {

        String appId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.app_id);
        ValidationUtils.isEquals(appId, dto.getAppid(), CredentialNotFoundException::new);

        // Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret_key);
        VendorService.verifyHash(oriRequest, dto.getToken(), secretKey);
    }
}
