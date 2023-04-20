package com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
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
            RawGameSession rawGameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetNSettleDto, rawGameSession);

            // 5. Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRefund(traceId, cancelBetNSettleDto.getTransferId(), rawGameSession, actionDto.getParams());

            vo.setBalance(betRefundEvent.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);
        
        } catch (AuthenticationException authenticationException) {
            vo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (BetNotFoundException betNotFoundException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            vo.setResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException invalidRequestException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (RecordNotFoundException recordNotFoundException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledGameException disabledGameException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(CancelBetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetNSettleDto dto, RawGameSession rawGameSession) throws DisabledVendorLineException,
    DisabledAgentPlayerException, InvalidPlayerException, DisabledGameException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(rawGameSession, dto.getUid());
    }
}
