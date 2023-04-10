package com.nextgen.gameaggregator.vendor.jdb.api.balance;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BalanceService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;

    public CommonVo balance(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BalanceDto balanceDto = HttpService.convertJsonToDto(actionDto.getParams(), BalanceDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // 2. Get vendor player details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getUid());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Construct VO
            vo.setBalance(balance);
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException exception) {
            vo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (InvalidAgentApiCredentialException exception) {
            vo.setResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException exception) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidPlayerException exception) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException exception) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException exception) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws InvalidPlayerException, InvalidRequestException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // 2. Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), InvalidRequestException::new);
    }
}
