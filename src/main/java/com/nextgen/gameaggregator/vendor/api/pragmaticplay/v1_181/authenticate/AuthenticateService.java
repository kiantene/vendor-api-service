package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.bet.BetDto;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialValueReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorCredentialReaderManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorCredentialValueReaderManager;
import com.nextgen.gameaggregator.vendor.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthenticateService {

    @Autowired
    public VendorCredentialReaderManager vendorCredentialReaderManager;

    @Autowired
    public VendorCredentialValueReaderManager vendorCredentialValueReaderManager;

    @Autowired
    private VendorPlayerAuthenticationRepository vendorPlayerAuthenticationRepository;

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

    public void validateRequest(AuthenticateDto dto, Class<AuthenticateDto> clazz) throws InvalidRequestException {
        Map<String, String> validationMap = new HashMap<String, String>();

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<AuthenticateDto>> violations = validator.validate(dto);
        for (ConstraintViolation<AuthenticateDto> violation : violations) {
            validationMap.put(violation.getPropertyPath().toString(), violation.getPropertyPath() + " " + violation.getMessage());
        }

        System.out.println("validationMap++++ "+validationMap);

        if(!validationMap.isEmpty()){
            throw new InvalidRequestException(validationMap.toString());
        }
    }

    public void validateHash(String hash, String secretKey, String requestBody) throws InvalidHashException {

        String modifiedQueryString = requestBody.replaceAll("(^|&)hash=.*?(&|$)", "$1$2");

        MultiValueMap<String, String> map = UriComponentsBuilder.newInstance()
                .query(modifiedQueryString)
                .build()
                .getQueryParams();

        String checkerHash = this.generateHash(map, secretKey);

        if(!hash.equals(checkerHash)){
            throw new InvalidHashException();
        }

    }

    private String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream()
                .sorted()
                .map(key -> key + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        payload += secret;
        return DigestUtils.md5Hex(payload);
    }

    public VendorPlayerAuthentication verifyToken(String token) throws AuthenticationException {
        VendorPlayerAuthentication authenticatedUser = new VendorPlayerAuthentication();

        authenticatedUser = vendorPlayerAuthenticationRepository.findByTraceId(token);

        Optional.ofNullable(authenticatedUser).orElseThrow(AuthenticationException::new);

        return authenticatedUser;
    }

    public String verifyCredential(Long vendorCredentialId) throws UnableToFindCredentialsException {
        Map<String, String> credentialMap = new HashMap<String, String>();
        List<VendorCredentialValueReader> vendorCredentialValueReader;
        VendorCredentialReader vendorCredentialReader;

        vendorCredentialReader = vendorCredentialReaderManager.findById(vendorCredentialId).orElse(null);

        //check credentials validity and latest version
        Optional.ofNullable(vendorCredentialReader).orElseThrow(UnableToFindCredentialsException::new);

        vendorCredentialValueReader = vendorCredentialValueReaderManager.findByVendorCredentialIdAndVersion(
                vendorCredentialId, vendorCredentialReader.getLatestVersion());

        //check credentials value validity
        Optional.ofNullable(vendorCredentialValueReader).orElseThrow(UnableToFindCredentialsException::new);

        VendorCredentialValueReader credentialValues = vendorCredentialValueReader.stream()
                .filter(key -> key.getKey().equals("secretKey"))
                .findFirst()
                .orElse(null);

        return credentialValues.getValue();
    }

    public BigDecimal getWalletBalanceFromGRPC(AuthenticateDto dto, String traceId, VendorPlayerAuthentication vpa) {
        //TODO: call operatorBetRequestGrpc.betRequest to get the balance of player from operator
        return new BigDecimal("1000");

        //prepare call to operator grpc
//        WalletBalanceGrpcVo serviceVo = this.operatorWalletBalanceGrpc.walletBalance(
//                vpa.getAgentId(),
//                vpa.getAgentPlayerId(),
//                vpa.getGameId(),
//                vpa.getCurrencyCode(),
//                traceId,
//                agentCredentialReaderManager.findByAgentId(vpa.getAgentId()).getId()
//        );
    }
}
