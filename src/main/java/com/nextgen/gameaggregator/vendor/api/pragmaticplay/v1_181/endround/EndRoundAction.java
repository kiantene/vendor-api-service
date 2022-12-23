package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.endround;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessBetHistoryRequest;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.vendor.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.exception.InvalidHashException;
import com.nextgen.gameaggregator.vendor.exception.InvalidRequestException;
import com.nextgen.gameaggregator.vendor.exception.UnableToFindCredentialsException;
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

import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.constant.ResponseCodes.INVALID_REQUEST;

@RestController
@RequestMapping(path = "api/v2/pragmaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
@RequestScope
public class EndRoundAction {

    @Autowired
    HttpServletRequest request;

    @Autowired
    EndRoundService endRoundService;

    @PostMapping(path = "endRound")
    public EndRoundVo endRound(EndRoundDto dto) {

        EndRoundVo response = new EndRoundVo();
        String errorMessage = "";

        try {
            // region temporary solution to convert to DTO
            String requestBody = request.getReader().lines().collect(Collectors.joining());
            dto = endRoundService.queryStringToDto(requestBody, EndRoundDto.class);
            log.info(dto.toString());
            // endregion

            // Validate request parameters from vendor
            endRoundService.validateRequest(dto, EndRoundDto.class);
            VendorPlayerAuthentication authenticatedUser = endRoundService.verifyToken(dto.getToken());

            // Validate hash from vendor
            String secretKey = endRoundService.verifyCredential(authenticatedUser.getVendorCredentialId());
            endRoundService.validateHash(dto.getHash(), secretKey, requestBody);

            // use the bet result round id to find the bet request info from log request
            SeamlessBetHistoryRequest seamlessBetRequest = endRoundService.getSeamlessBetRequestId(dto);

            if (seamlessBetRequest == null){
                // if not finding bet request info, will create as "others" record for extra handling
                endRoundService.createRawBetHistorySeamlessOthersCouchBase(requestBody, authenticatedUser.getVendorCurrencyCode());
            } else {
                // if found the bet request info, will create as success result record (kafka topic)
                endRoundService.createRecordToKafkaBetHistoryTopic(seamlessBetRequest.getBetHistoryId(), authenticatedUser, requestBody);
            }

            // no matter bet request is found or not, will still proceed to send to operator
            // Call bet request operator GRPC to get the balance of the player
            String traceId = UUID.randomUUID().toString();
            BigDecimal balance = endRoundService.getBetRequestBalanceFromGRPC(dto, traceId, authenticatedUser, seamlessBetRequest);

            response.setError(ResponseCodes.SUCCESS);
            response.setCash(balance);
            response.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            response.setError(INVALID_REQUEST);
            errorMessage = " || "+invalidRequestException.getMessage();

        } catch (AuthenticationException authenticationException) {
            response.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

        } catch (InvalidHashException invalidHashException) {
            response.setError(ResponseCodes.INVALID_HASH);

        } catch (Exception exception) { // any other exception encountered
            response.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            errorMessage = " || "+exception;

        } finally {
            response.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(response.getError())+errorMessage);
        }

        return response;
    }
}
