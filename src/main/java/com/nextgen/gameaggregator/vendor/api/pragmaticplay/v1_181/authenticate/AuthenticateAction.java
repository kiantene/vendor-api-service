package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.vendor.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.exception.InvalidHashException;
import com.nextgen.gameaggregator.vendor.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "api/v2/pragmaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class AuthenticateAction {
    @Autowired
    HttpServletRequest request;

    @Autowired
    AuthenticateService authenticateService;

    @PostMapping(path = "authenticate")
    public AuthenticateVo authenticate(AuthenticateDto dto) {
        AuthenticateVo response = new AuthenticateVo();

        try {
            // region temporary solution to convert to DTO
            String requestBody = request.getReader().lines().collect(Collectors.joining());
            dto = authenticateService.queryStringToDto(requestBody, AuthenticateDto.class);
            log.info(dto.toString());
            // endregion

            // Validate request parameters from vendor
            authenticateService.validateRequest(dto);
            authenticateService.validateHash(dto.getHash(), requestBody);
            VendorPlayerAuthentication authenticatedUser = authenticateService.verifyToken(dto.getToken());
            BigDecimal balance = authenticateService.getWalletBalance(authenticatedUser);

            response.setError(ResponseCodes.SUCCESS);
            response.setUserId(authenticatedUser.getVendorPlayerUsername());
            response.setCurrency(authenticatedUser.getCurrencyCode());
            response.setCash(balance);
            response.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            response.setError(ResponseCodes.INVALID_REQUEST);

        } catch (InvalidHashException invalidHashException) {
            response.setError(ResponseCodes.INVALID_HASH);

        } catch (AuthenticationException authenticationException) {
            response.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (Exception exception) { // any other exception encountered
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } finally {
            response.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(response.getError()));
        }

        return response;
    }
}
