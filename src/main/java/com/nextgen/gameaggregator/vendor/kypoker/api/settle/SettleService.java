package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.constant.RoomCode;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettleService {
    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;
    private final HttpService httpService;
    private final WalletTransactionService walletTransactionService;


    public SettleService(
            WalletService walletService,
            ValidationService validationService,
            GameSessionService gameSessionService,
            VendorService vendorService,
            OperatorWalletService operatorWalletService,
            WalletRequestService walletRequestService,
            HttpService httpService,
            WalletTransactionService walletTransactionService) {

        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
        this.httpService = httpService;
        this.walletTransactionService = walletTransactionService;
    }
    public CommonVo settle(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam, Long timeStamp) {

        // Construct VO
        CommonVo vo = new CommonVo();
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        WalletTransaction walletTransaction = null;
        Integer roomMode = 0;
        SettleDto settleDto = null;
        String errorMessage = "";
        ResponseObjectDto d = new ResponseObjectDto();

        try {
            // Convert original request body into dto
            settleDto = HttpService.convertQueryStringToDto(decryptedParam, SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(settleDto.getAccount());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            settleDto.setTimeStamp(timeStamp);

            // Check game code to use normal flow or credit debit
            roomMode = settleDto.getRoomMode();

            //Normal Flow
            if(settleDto.getRoomMode() == RoomCode.CODE2 || settleDto.getRoomMode() == RoomCode.CODE3) {
                ResultType resultType = (settleDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.BET_WIN : ResultType.BET_LOSE;
                BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService, httpRequestLog);
                d.setMoney(balance);
            }
            //Credit Debit flow
            else if(settleDto.getRoomMode() == RoomCode.CODE1 || settleDto.getRoomMode() == RoomCode.CODE4){
                walletTransaction = walletTransactionService.getByRoundIdAndVendorPlayerUsername(settleDto.getGameNo(), settleDto.getAccount());

                if(walletTransaction !=null ) {
                    WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                    vendorService.dataCreditMapper(currentWalletRequest, settleDto, gameSession);
                    walletRequest = operatorWalletService.betCredit(currentWalletRequest);
                    d.setMoney(walletRequest.getBalanceAfter());

                }
                else{
                    throw new BetNotFoundException() ;

                }
            }

            d.setCode(ResponseCodes.SUCCESS);
            d.setAccount(gameSession.getVendorPlayerUsername());

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            d.setCode(ResponseCodes.DUPLICATE);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            errorMessage = betResultIdempotentViolationException.toString();


        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            d.setCode(ResponseCodes.PROCESSING);
            httpService.logError(httpRequestLog, transactionStillProcessingException);
            errorMessage = transactionStillProcessingException.toString();


        } catch (BetNotFoundException betNotFoundException) {
            d.setCode(ResponseCodes.BET_NOT_FOUND);
            httpService.logError(httpRequestLog, betNotFoundException);
            errorMessage = betNotFoundException.toString();


        } catch (InvalidRequestException invalidRequestException) {
            d.setCode(ResponseCodes.INVALID_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);
            errorMessage = invalidRequestException.toString();


        } catch (Exception e){
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, e);
            errorMessage = e.toString();

        } finally {
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setM(EndPoints.API_ENDPOINT);
            vo.setD(d);

            if (roomMode == RoomCode.CODE1 || roomMode == RoomCode.CODE4){
                walletRequest.setErrorMessage(errorMessage);
                walletRequestService.end(walletRequest, httpRequestLog, vo);
            }else {
                httpService.end(httpRequestLog, vo);
            }
        }
        return vo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            AuthenticationException,
            InvalidRequestException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAccount());

        // Verify vendor gameCode, currency and platform
        String[] parts = gameSession.getVendorGameCode().split("_");
        int mType = Integer.parseInt(parts[0]);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccount(), InvalidRequestException::new);
        ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getKindId()), InvalidRequestException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), InvalidRequestException::new);

    }

}