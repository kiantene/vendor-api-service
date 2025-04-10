package com.nextgen.gameaggregator.vendor.kypoker.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.constant.*;

@Service
public class BetService {

    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;
    private final HttpService httpService;

    public BetService(GameService gameService,
                      WalletService walletService,
                      ValidationService validationService,
                      GameSessionService gameSessionService,
                      VendorService vendorService,
                      OperatorWalletService operatorWalletService,
                      WalletRequestService walletRequestService,
                      HttpService httpService) {
        this.gameService = gameService;
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

        try {
            // Convert original request body into dto
            BetDto betDto = HttpService.convertQueryStringToDto(decryptedParam, BetDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(betDto.getAccount());

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(String.valueOf(betDto.getKindId()), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            // Vendor does not provide bet timestamp
            betDto.setTimeStamp(timeStamp);

            // Determine game mode to use normal bet flow or Credit/Debit
            ResponseObjectDto d = new ResponseObjectDto();

            // Normal flow
            if(betDto.getRoomMode() == RoomCode.CODE2 || betDto.getRoomMode() == RoomCode.CODE3){

                BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, actionDto, httpRequestLog);

                d.setCode(ResponseCodes.SUCCESS);
                d.setAccount(gameSession.getVendorPlayerUsername());
                d.setMoney(betEvent.getLastBalance());
                d.setRoomMode(betDto.getRoomMode());
                httpService.end(httpRequestLog, vo);

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

            // Construct VO
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BET);
            vo.setD(d);

        } catch (Exception e){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setStatus(ResponseCodes.INTERNAL_ERROR);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BET);
            vo.setD(d);

        }finally {
            if (walletRequest == null){
                httpService.end(httpRequestLog, vo);
            }else {
                walletRequestService.end(walletRequest, httpRequestLog, vo);
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
