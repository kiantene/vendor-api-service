package com.nextgen.gameaggregator.vendor.jdb.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CancelBetService {

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

    public CommonVo cancelBet(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            CancelBetDto cancelBetDto = HttpService.convertJsonToDto(actionDto.getParams(), CancelBetDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(cancelBetDto);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(cancelBetDto.getUid());
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(cancelBetDto.getRefTransferIds().get(0).toString(), vendorPlayer.getVendorId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetDto, gameSession);

            // 5. Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRollback(traceId, cancelBetDto.getRefTransferIds().get(0).toString(), gameSession, actionDto.getParams());

            vo.setBalance(betRefundEvent.getLastBalance());
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
            if (invalidRequestException.getValidation() != null) {
                String violation = invalidRequestException.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCode.INVALID_REQUEST_PARAMETER); // if there's no value, set it to the default invalid request parameter
                vo.setErrorResponseCode(violation);
            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
            }
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (RecordNotFoundException recordNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (DisabledGameException disabledGameException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException,
            CurrencyNotSupportedException, InvalidPlayerException, DisabledGameException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, dto.getUid());

        // Verify vendor currency
       ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
