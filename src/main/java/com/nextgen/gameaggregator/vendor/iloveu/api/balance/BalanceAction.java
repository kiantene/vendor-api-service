package com.nextgen.gameaggregator.vendor.iloveu.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Credentials;
import com.nextgen.gameaggregator.vendor.iloveu.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.iloveu.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.iloveu.service.VendorService;
import com.nextgen.gameaggregator.vendor.iloveu.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.iloveu.vo.DataVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo responseVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into balanceDto
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            //Get GameSession by token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getLoginId().toLowerCase());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //return success respond
            responseVo.getDataVo().setBalance(balance.setScale(2, RoundingMode.DOWN));


        } catch (InvalidAgentApiCredentialException |
                 VendorCurrencyNotSupportException |
                 AuthenticationException |
                 InvalidEncryptionException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 JsonProcessingException |
                 DisabledVendorLineException |
                 CredentialNotFoundException generalException) {
            responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, generalException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                responseVo.setResponseCode(
                        invalidRequestException.getValidation()
                                .entrySet()
                                .stream()
                                .findFirst()
                                .map(Map.Entry::getValue) // get the value of the first element
                                .orElse(ResponseCodes.INVALID_PARAMETER)
                );
            } else {
                responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        return responseVo;

    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.isEquals("GetSingleWallet", dto.getMethod(), InvalidRequestException::new);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidEncryptionException,
            CredentialNotFoundException,
            InvalidRequestException {

        //Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        //Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        //Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        //Verify vendor SN
        String sn = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SERIAL_NUMBER);
        ValidationUtils.isEquals(sn, dto.getSn(), InvalidRequestException::new);

        //Generate encryptString
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_SECRET_KEY);
        String encryptString = dto.getId() + dto.getMethod() + dto.getSn() + dto.getLoginId() + apiKey;

        //Verify signature
        String md5Param = vendorService.md5(encryptString);
        if (!dto.getSignature().toUpperCase().equals(md5Param.toUpperCase())) {
            throw new InvalidRequestException(vendorService.invalidRequestRespond(ResponseCodes.INVALID_SIGNATURE));
        }

    }

}
