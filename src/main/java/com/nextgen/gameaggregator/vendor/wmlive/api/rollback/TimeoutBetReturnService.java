package com.nextgen.gameaggregator.vendor.wmlive.api.rollback;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.wmlive.api.action.GeneralActionDto;
import com.nextgen.gameaggregator.vendor.wmlive.api.vo.DataVo;
import com.nextgen.gameaggregator.vendor.wmlive.api.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.wmlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.wmlive.constant.ResponseCode;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.CredentialException;

@Service
public class TimeoutBetReturnService {
    private final VendorService vendorService;
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;

    @Autowired
    public TimeoutBetReturnService(VendorService vendorService, HttpService httpService,
                                   VendorLineService vendorLineService,
                                   GameSessionService gameSessionService,
                                   WalletService walletService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;

    }

    public ResponseVo timeoutBetReturn(GeneralActionDto dto, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        ResponseVo responseVo = new ResponseVo();
        TimeoutBetReturnDto timeoutBetReturnDto = null;
        GameSession gameSession = new GameSession();

        try {
            timeoutBetReturnDto = new ModelMapper().map(dto, TimeoutBetReturnDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(timeoutBetReturnDto);

            try {
                // Get GameSession with username
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(timeoutBetReturnDto.getUser());
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(timeoutBetReturnDto.getUser()); //generate new token
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // Verify remaining parameters (Verify against database values)
            this.doVerification(timeoutBetReturnDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            WalletRequest walletRequest = walletService.processRollback(timeoutBetReturnDto, gameSession, vendorService, httpRequestLog);

            // Set response
            DataVo timeoutBetReturnVo = new DataVo(timeoutBetReturnDto, walletRequest.getBalanceAfter());
            responseVo.setResult(timeoutBetReturnVo);

        } catch (BetNotFoundException | BetResultIdempotentViolationException | InvalidOperatorResponseException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR_BLOCKED);
        } catch (CredentialNotFoundException | CredentialException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.CREDENTIAL_ERROR);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR);
        }

        return responseVo;
    }

    private void doValidation(TimeoutBetReturnDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(TimeoutBetReturnDto dto, GameSession gameSession) throws CredentialNotFoundException, CredentialException {

        //5. Verify received signature is same with credential signature
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE);
        ValidationUtils.isEquals(token, dto.getSignature(), CredentialException::new);
    }

}
