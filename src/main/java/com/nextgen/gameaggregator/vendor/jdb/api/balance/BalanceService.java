package com.nextgen.gameaggregator.vendor.jdb.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.api.terminate.TerminateService;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BalanceService {

    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final TerminateService terminateService;
    private final HttpService httpService;

    public BalanceService(GameServiceImpl gameService,
                          WalletService walletService,
                          ValidationService validationService,
                          TerminateService terminateService,
                          HttpService httpService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.terminateService = terminateService;
        this.httpService = httpService;
    }

    public CommonVo balance(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog, HttpServletRequest request, VendorLine vendorLine) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BalanceDto balanceDto = HttpService.convertJsonToDto(actionDto.getParams(), BalanceDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // 2. Get vendor player details
            GameSession gameSession = gameService.getGameSessionByUsername(balanceDto.getUid());

            //if player's session got terminated
            if (gameSession.getStatus() == 0) {
                terminateService.terminate(gameSession, request, vendorLine);
                throw new AuthenticationException();
            }

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, actionDto.getHttpRequestLog());

            // Construct VO
            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (InvalidAgentApiCredentialException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidPlayerException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            httpService.logError(httpRequestLog, disabledVendorLineException);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws InvalidPlayerException, InvalidRequestException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());
    }
}
