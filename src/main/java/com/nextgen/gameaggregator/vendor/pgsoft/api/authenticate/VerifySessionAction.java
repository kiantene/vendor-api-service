package com.nextgen.gameaggregator.vendor.pgsoft.api.authenticate;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class VerifySessionAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

    @Autowired
    private VendorGameRepository vendorGameRepository;

    @PostMapping(path = Endpoints.AUTHENTICATE)
    public ResponseVo<VerifySessionVo> authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Construct Vo
        ResponseVo<VerifySessionVo> parentResponseVo = new ResponseVo<>();

        try {

            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            VerifySessionDto dto = HttpService.convertQueryStringToDto(body, VerifySessionDto.class);
            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            // 2. Verify session token - Need to validate whether game session expired
            // If Token has been tampered, then AuthenticationException will be thrown
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());
            // x. Check credential line inactive
            agentApiCredentialService.getAgentApiCredential(gameSession.getAgentId());
            // 3. Validate vendor game code
            VendorService.validateVendorGameCode(String.valueOf(dto.getGameId()), gameSession.getVendorGameCode());
            // x. Validate is game disabled
            VendorGame game = vendorGameRepository.findByVendorGameCodeAndVendorId(String.valueOf(dto.getGameId()), gameSession.getVendorId());
            VendorService.validateGameStatus(game);
            // 4. Retrieve vendor line operatorToken and secretKey for validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
            String operatorToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_TOKEN);
            // 5. Validate request operatorToken and secretKey
            VendorService.validateOperatorTokenAndSecretKey(dto.getOperatorToken(), dto.getSecretKey(), operatorToken, secretKey);


            // Fill VO required values
            VerifySessionVo responseVo = new VerifySessionVo();
            parentResponseVo.setData(responseVo);
            responseVo.setPlayerName(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());


        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_OPERATOR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_OPERATOR));

        } catch (GameNotSupportedException gameNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.GAME_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.GAME_DOES_NOT_EXIST));

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));

        } catch (CredentialNotFoundException credentialNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        }  catch (NoAvailableLineException noAvailableLineException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (Exception exception) { // any other exception encountered
            parentResponseVo.setErrorCode(ResponseCodes.INTERNAL_SERVER_ERROR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INTERNAL_SERVER_ERROR));
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, parentResponseVo);
        }

        //
        return parentResponseVo;
    }
}
