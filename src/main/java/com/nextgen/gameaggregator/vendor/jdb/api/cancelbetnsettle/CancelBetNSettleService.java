package com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetRollbackEvent;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.service.BetHistoryService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
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
            BetRollbackEvent betRollbackEvent = walletService.processRollback(traceId, cancelBetNSettleDto.getTransferId(), gameSession, actionDto.getParams());

            vo.setBalance(betRollbackEvent.getLastBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);
        
        } catch (AuthenticationException authenticationException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (BetNotFoundException betNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException invalidRequestException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (RecordNotFoundException recordNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
//        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
//            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledGameException disabledGameException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
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
        validationService.validateIllegibleBet(gameSession, dto.getUid());
    }
}
