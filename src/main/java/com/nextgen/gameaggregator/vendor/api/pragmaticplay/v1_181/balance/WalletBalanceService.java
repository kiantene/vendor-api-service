package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.vendor.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.exception.InvalidHashException;
import com.nextgen.gameaggregator.vendor.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

@Service
@Slf4j
public class WalletBalanceService {

    public <T> T queryStringToDto(String queryString, Class<T> clazz) {

        log.info(queryString);

        HashMap<String, Object> queryParameterMap = new HashMap<String, Object>();
        String[] fields = queryString.split("&");

        for (int i = 0; i < fields.length; ++i) {
            String[] kv = fields[i].split("=");
            if (2 == kv.length) {
                queryParameterMap.put(kv[0], kv[1]);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        T t = mapper.convertValue(queryParameterMap, clazz);

        return t;
    }

    public void validateRequest(WalletBalanceDto dto) throws InvalidRequestException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        if (!validator.validate(dto).isEmpty()) { // Missing request parameters
            throw new InvalidRequestException();
        }
    }

    public void validateHash(String hash, String requestBody) throws InvalidHashException {
        if (hash.compareTo("%20") == 0) {
            throw new InvalidHashException();
        }
    }

    public VendorPlayerAuthentication verifyToken(String token) throws AuthenticationException {
        VendorPlayerAuthentication authenticatedUser = new VendorPlayerAuthentication();

        Optional.ofNullable(authenticatedUser).orElseThrow(AuthenticationException::new);

        //TODO: remove below code after implementing database
        authenticatedUser.setCurrencyCode("CNY");
        authenticatedUser.setVendorPlayerUsername("testusername");

        return authenticatedUser;
    }

    public BigDecimal getWalletBalanceFromGRPC(VendorPlayerAuthentication authenticatedUser) {
        //TODO: call operatorWalletBalanceGrpc.walletBalance to get the balance of player from operator
        return new BigDecimal("1000");
    }

}
