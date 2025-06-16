package com.nextgen.gameaggregator.vendor.kypoker.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.constant.*;

import java.math.BigDecimal;

@Service
public class BetService {

    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;
    private final HttpService httpService;

    public BetService(
                      WalletService walletService,
                      ValidationService validationService,
                      GameSessionService gameSessionService,
                      VendorService vendorService,
                      OperatorWalletService operatorWalletService,
                      WalletRequestService walletRequestService,
                      HttpService httpService) {
        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
        this.httpService = httpService;
    }

    public CommonVo bet(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam, Long timeStamp) {
        // Construct VO
        CommonVo vo = new CommonVo();
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        Integer roomMode = null;
        BetDto betDto = null;
        String errorMessage = "";
        ResponseObjectDto d = new ResponseObjectDto();

        try {
            // Convert original request body into dto
            betDto = HttpService.convertQueryStringToDto(decryptedParam, BetDto.class);

            roomMode = betDto.getRoomMode();

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(betDto.getAccount());

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(String.valueOf(betDto.getKindId()), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            // Vendor does not provide bet timestamp
            betDto.setTimeStamp(timeStamp);

            // Normal flow
            if(betDto.getRoomMode() == RoomCode.CODE2 || betDto.getRoomMode() == RoomCode.CODE3){
                BigDecimal betAction = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService,httpRequestLog);

                d.setCode(ResponseCodes.SUCCESS);
                d.setAccount(gameSession.getVendorPlayerUsername());
                d.setMoney(betAction);

            }

            // Credit Debit flow
            else if(betDto.getRoomMode() == RoomCode.CODE1 || betDto.getRoomMode() == RoomCode.CODE4){

                walletRequest = WalletRequestService.init(httpRequestLog);
                WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                vendorService.dataDebitMapper(currentWalletRequest, betDto, gameSession);

                walletRequest = operatorWalletService.betDebit(currentWalletRequest);

                d.setCode(ResponseCodes.SUCCESS);
                d.setAccount(gameSession.getVendorPlayerUsername());
                d.setMoney(walletRequest.getBalanceAfter());
                d.setRoomMode(betDto.getRoomMode());
            }

        } catch (InsufficientBalanceException insufficientBalanceException) {
            d.setCode(ResponseCodes.STATUS_SUCCESS);
            httpService.logError(httpRequestLog, insufficientBalanceException);
            errorMessage = insufficientBalanceException.toString();

        }  catch (InvalidOperatorResponseException insufficientBalanceException) {

            if (insufficientBalanceException.getOperatorStatus() == 11) {
                d.setCode(ResponseCodes.INSUFFICIENT_FUNDS);
                httpService.logError(httpRequestLog, new InsufficientBalanceException());
                errorMessage = insufficientBalanceException.toString();

            } else {
                d.setCode(ResponseCodes.INTERNAL_ERROR);
                httpService.logError(httpRequestLog, insufficientBalanceException);
                errorMessage = insufficientBalanceException.toString();
            }

        } catch (BetResultIdempotentViolationException duplicateRequestException) {
            d.setCode(ResponseCodes.DUPLICATE);
            httpService.logError(httpRequestLog, duplicateRequestException);
            errorMessage = duplicateRequestException.toString();

        }   catch (InvalidRequestException | CurrencyNotSupportedException invalidRequestException) {
            d.setCode(ResponseCodes.INVALID_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);
            errorMessage = invalidRequestException.toString();

        } catch (Exception e){
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, e);
            errorMessage = e.toString();

        }finally {
            vo.setM(EndPoints.API_ENDPOINT);
            vo.setS(ResponseCodes.GET_BET);
            vo.setD(d);

            if (roomMode == RoomCode.CODE1 || roomMode == RoomCode.CODE4) {
                walletRequest.setErrorMessage(errorMessage);
                walletRequestService.end(walletRequest, httpRequestLog, vo);
            } else {
                httpService.end(httpRequestLog, vo);  // Ensure this runs even after an exception
            }
        }
        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAccount());

        // Verify vendor gameCode, currency and platform
        String[] parts = gameSession.getVendorGameCode().split("_");
        int mType = Integer.parseInt(parts[0]);
        ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getKindId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
