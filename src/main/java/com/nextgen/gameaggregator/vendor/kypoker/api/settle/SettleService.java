package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
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
    private final RequestIdempotentLogService requestIdempotentLogService;


    public SettleService(
            WalletService walletService,
            ValidationService validationService,
            GameSessionService gameSessionService,
            VendorService vendorService,
            OperatorWalletService operatorWalletService,
            WalletRequestService walletRequestService,
            HttpService httpService,
            RequestIdempotentLogService requestIdempotentLogService) {

        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }
    public CommonVo settle(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam, Long timeStamp) {

        // Construct VO
        CommonVo vo = new CommonVo();
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        Integer roomMode = 0;
        Boolean isRequestExists = false;
        SettleDto settleDto = null;

        try {
            // Convert original request body into dto
            settleDto = HttpService.convertQueryStringToDto(decryptedParam, SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(settleDto.getAccount());


            if (requestIdempotentLogService.checkExists(settleDto, settleDto.getAccount()) == null) {

                requestIdempotentLogService.create(settleDto, settleDto.getAccount());

            } else {

                isRequestExists = true;
                throw new TransactionStillProcessingException();

            }

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            settleDto.setTimeStamp(timeStamp);
            ResponseObjectDto d = new ResponseObjectDto();

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
                WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                vendorService.dataCreditMapper(currentWalletRequest, settleDto, gameSession);
                walletRequest = operatorWalletService.betCredit(currentWalletRequest);
                d.setMoney(walletRequest.getBalanceAfter());
            }

            d.setCode(ResponseCodes.SUCCESS);
            d.setAccount(gameSession.getVendorPlayerUsername());

            // Construct VO
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);

            if (gameSession.getVendorPlayerId().equals(26225523)){
                vo.setM(null);
                vo.setS(null);
                vo.setD(null);
            }

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(9);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);


        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(11);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);
            httpService.logError(httpRequestLog, transactionStillProcessingException);


        } catch (BetNotFoundException betNotFoundException) {
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(12);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);
            httpService.logError(httpRequestLog, betNotFoundException);


        } catch (InvalidRequestException invalidRequestException) {
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(5);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);
            httpService.logError(httpRequestLog, invalidRequestException);


        } catch (Exception e){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);
            httpService.logError(httpRequestLog, e);

        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(settleDto, settleDto.getAccount());
            }
            if (roomMode == RoomCode.CODE1 || roomMode == RoomCode.CODE4){
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

    private void doVerification(SettleDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAccount());

        // Verify vendor gameCode, currency and platform
        String[] parts = gameSession.getVendorGameCode().split("_");
        int mType = Integer.parseInt(parts[0]);
        ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

}