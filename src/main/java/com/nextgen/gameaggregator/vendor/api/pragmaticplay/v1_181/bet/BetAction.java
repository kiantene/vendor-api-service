package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.bet;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryRequest;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.vendor.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "api/v2/pragmaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
@RequestScope
public class BetAction {

    @Autowired
    HttpServletRequest request;

    @Autowired
    BetService betService;

    @PostMapping(path = "bet")
    public BetVo authenticate(BetDto dto) {

        BetVo response = new BetVo();

        try {
            // region temporary solution to convert to DTO
            String requestBody = request.getReader().lines().collect(Collectors.joining());
            dto = betService.queryStringToDto(requestBody, BetDto.class);
            log.info(dto.toString());
            // endregion

            // Validate request parameters from vendor
            betService.validateRequest(dto);
            betService.validateHash(dto.getHash(), requestBody);
            VendorPlayerAuthentication authenticatedUser = betService.verifyToken(dto.getToken());

            // Get seamless bet request Id
            String seamlessBetRequestId = betService.getSeamlessBetRequestId(dto);

            if (seamlessBetRequestId == null){
                // If not finding seamless bet request id, then create one
                seamlessBetRequestId = betService.createLogSeamlessBetHistoryRequest(dto, requestBody);
            }

            // Create raw seamless bet history request data for data transforming
            betService.createRawBetHistorySeamlessRequest(seamlessBetRequestId, authenticatedUser, requestBody);

            // Call bet request operator GRPC to get the balance of the player
            String traceId = UUID.randomUUID().toString();
            BigDecimal balance = betService.getBetRequestBalanceFromGRPC(dto, traceId, authenticatedUser, seamlessBetRequestId);

            response.setTransactionId(traceId.replace("-", ""));
            response.setUsedPromo(BigDecimal.ZERO);
            response.setCurrency(authenticatedUser.getVendorCurrencyCode());
            response.setError(ResponseCodes.SUCCESS);
            response.setCash(balance);
            response.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            response.setError(ResponseCodes.INVALID_REQUEST);

        } catch (InvalidHashException invalidHashException) {
            response.setError(ResponseCodes.INVALID_HASH);

        } catch (AuthenticationException authenticationException) {
            response.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (CreateLogSeamlessBetHistoryException createLogSeamlessBetHistoryException) {
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_RETRY);

        } catch (CreateRawBetHistorySeamlessException createRawBetHistorySeamlessException) {
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_RETRY);

        } catch (Exception exception) { // any other exception encountered
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } finally {
            response.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(response.getError()));
        }

        return response;
    }
}
