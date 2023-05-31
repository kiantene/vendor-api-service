package com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

@Service
public class CancelBetNSettleService {

    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    public CommonVo cancelBetNSettle(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            CancelBetNSettleDto cancelBetNSettleDto = HttpService.convertJsonToDto(actionDto.getParams(), CancelBetNSettleDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(cancelBetNSettleDto);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(cancelBetNSettleDto.getUid());
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(cancelBetNSettleDto.getTransferId(), vendorPlayer.getVendorId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetNSettleDto, gameSession);

            // 5. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelBetNSettleDto, gameSession, vendorService);

            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException |
                 InvalidPlayerException playerNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (BetNotFoundException |
                 RecordNotFoundException |
                 DisabledAgentPlayerException |
                 DisabledVendorLineException |
                 DisabledGameException failedException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException |
                 InvalidRequestException |
                 JsonProcessingException invalidRequestException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (Exception exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(CancelBetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetNSettleDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, InvalidPlayerException, DisabledGameException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());
    }
}
