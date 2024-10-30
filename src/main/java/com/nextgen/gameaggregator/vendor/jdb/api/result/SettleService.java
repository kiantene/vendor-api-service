package com.nextgen.gameaggregator.vendor.jdb.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(settleDto.getUid());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto,
                    (settleDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.END, vendorService, actionDto.getHttpRequestLog());

            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            vo.setBalance(betResultIdempotentViolationException.getBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (BetNotFoundException | DisabledAgentPlayerException |
                 DisabledVendorLineException | DisabledGameException e) {
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } catch (TransactionStillProcessingException | InvalidOperatorResponseException cannotCancelException) {
            vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (InvalidAgentApiCredentialException | JsonProcessingException |
                 GameNotSupportedException | CurrencyNotSupportedException InvalidRequestException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

        } catch (InvalidRequestException invalidRequestException) {
            if (invalidRequestException.getValidation() != null && !invalidRequestException.getValidation().isEmpty()) {
                String violation = invalidRequestException.getValidation().entrySet().iterator().next().getValue();
                vo.setErrorResponseCode(violation);
            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
            }

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
        //validationService.validateEligibleBet(gameSession, dto.getUid());

        // Verify vendor gameCode, currency
        //String[] parts = gameSession.getVendorGameCode().split("_");
        //int mType = Integer.parseInt(parts[1]);
        //ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        //ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify game category
        //if (!GameCategory.CATEGORY.containsValue(dto.getGType())) throw new InvalidRequestException();
    }
}
