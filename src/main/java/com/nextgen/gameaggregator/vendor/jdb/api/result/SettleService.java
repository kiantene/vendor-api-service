package com.nextgen.gameaggregator.vendor.jdb.api.result;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.GameCategory;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SettleService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    public CommonVo settle(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            SettleDto settleDto = HttpService.convertJsonToDto(actionDto.getParams(), SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleDto.getUid(), settleDto.getMType().toString());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, 
            (settleDto.getNetWin().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE, vendorService, actionDto.getParams());
            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (BetNotFoundException betNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
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
        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (GameNotSupportedException gameNotSupportedException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (DisabledGameException disabledGameException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (Exception exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleDto dto, GameSession gameSession) throws DisabledAgentPlayerException,
    DisabledVendorLineException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException, 
    InvalidRequestException, InvalidPlayerException, AuthenticationException {
       //validate vendor username, agent vendor line, player status, and game status
       validationService.validateIllegibleBet(gameSession, dto.getUid());

       // Verify vendor gameCode, currency
       ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
       ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

       // Verify game category
       if (!GameCategory.CATEGORY.containsValue(dto.getGType())) throw new InvalidRequestException();
   }
}
