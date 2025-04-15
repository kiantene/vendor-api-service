package com.nextgen.gameaggregator.vendor.kypoker.api.cancel;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
public class CancelService {

    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletTransactionService walletTransactionService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;

    public CancelService(GameService gameService,
                         WalletService walletService,
                         ValidationService validationService,
                         GameSessionService gameSessionService,
                         VendorService vendorService,
                         HttpService httpService,
                         WalletTransactionService walletTransactionService,
                         WalletRequestService walletRequestService, OperatorWalletService operatorWalletService) {
        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.walletTransactionService = walletTransactionService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
    }

    public CommonVo cancel(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam,Long timeStamp)
            throws AuthenticationException
    {
        // Construct VO
        CommonVo vo = new CommonVo();
        CancelDto cancelDto = null;
        BigDecimal balance = null;
        WalletTransaction walletTransaction = null;
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        try {
            // Convert original request body into dto
            cancelDto = HttpService.convertQueryStringToDto(decryptedParam, CancelDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(cancelDto);

            // 2. Verify session token
            GameSession gameSession;
            gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(cancelDto.getAccount()); //token check

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelDto, gameSession);

            cancelDto.setTimeStamp(timeStamp);

            // 4. Send refund to Operator
            try {
                balance = walletService.processRollback(traceId, cancelDto, gameSession, vendorService, httpRequestLog);

            } catch (BetNotFoundException e) {
                walletRequest = WalletRequestService.init(httpRequestLog);
                String externalTransactionId = cancelDto.getOrderId();
                walletTransaction = walletTransactionService.getByVendorIdAndExternalTransactionId(gameSession.getVendorId(), externalTransactionId);
                this.dataMapper(walletRequest,cancelDto,gameSession);
                walletRequest = operatorWalletService.debitRefundByExternalTransactionId(walletRequest);

                if (walletTransaction != null) {
                    ResponseObjectDto d = new ResponseObjectDto();
                    d.setCode(ResponseCodes.SUCCESS);
                    d.setStatus(ResponseCodes.CODE1);
                    vo.setM(EndPoints.LAUNCH_GAME);
                    vo.setS(ResponseCodes.CANCEL);
                    vo.setD(d);

                } else {
                    throw new BetNotFoundException();
                }
            }

            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.SUCCESS);
            d.setStatus(ResponseCodes.CODE1);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.CANCEL);
            vo.setD(d);

        }
        catch (Exception e){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            d.setCode(ResponseCodes.CODE2);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.CANCEL);
            vo.setD(d);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(CancelDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelDto dto, GameSession gameSession) throws DisabledVendorLineException,
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

    private void dataMapper (WalletRequest walletRequest, CancelDto cancelDto , GameSession gameSession)
    {
        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(cancelDto.getOrderId());
        walletRequest.setRoundId(cancelDto.getRoundId());
        walletRequest.setVendorGameCode(cancelDto.getGameId());
        walletRequest.setTimestamp(cancelDto.getTimeStamp());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(cancelDto.getOrderId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        BigDecimal amount = cancelDto.getMoney();
        walletRequest.setTransferAmount(amount);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
    }
}
