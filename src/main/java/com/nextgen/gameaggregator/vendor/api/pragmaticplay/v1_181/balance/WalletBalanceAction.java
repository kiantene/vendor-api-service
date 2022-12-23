package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.balance;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.UnableToFindCredentialsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "api/v2/pragmaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class WalletBalanceAction {

    @Autowired
    HttpServletRequest request;

    @Autowired
    WalletBalanceService walletBalanceService;

    @PostMapping(path = "balance")
    public WalletBalanceVo walletBalance(WalletBalanceDto dto) {
        WalletBalanceVo response = new WalletBalanceVo();
        String errorMessage = "";

        try {
            // region temporary solution to convert to DTO
            String requestBody = request.getReader().lines().collect(Collectors.joining());
            dto = walletBalanceService.queryStringToDto(requestBody, WalletBalanceDto.class);
            log.info(dto.toString());
            // endregion

            // Validate request parameters from vendor
            walletBalanceService.validateRequest(dto, WalletBalanceDto.class);
            VendorPlayerAuthentication authenticatedUser = walletBalanceService.verifyToken(dto.getToken());

            // Validate hash from vendor
            String secretKey = walletBalanceService.verifyCredential(authenticatedUser.getVendorCredentialId());
            walletBalanceService.validateHash(dto.getHash(), secretKey, requestBody);

            // Call wallet balance operator GRPC to get the balance of the player
            String traceId = UUID.randomUUID().toString();
            BigDecimal balance = walletBalanceService.getWalletBalanceFromGRPC(dto, traceId, authenticatedUser);

            response.setError(ResponseCodes.SUCCESS);
            response.setCurrency(authenticatedUser.getCurrencyCode());
            response.setCash(balance);
            response.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            response.setError(ResponseCodes.INVALID_REQUEST);
            errorMessage = " || "+invalidRequestException.getMessage();

        } catch (AuthenticationException authenticationException) {
            response.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } catch (InvalidSignatureException invalidSignatureException) {
            response.setError(ResponseCodes.INVALID_HASH);

        } catch (Exception exception) { // any other exception encountered
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } finally {
            response.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(response.getError())+errorMessage);
        }

        return response;
    }

}
