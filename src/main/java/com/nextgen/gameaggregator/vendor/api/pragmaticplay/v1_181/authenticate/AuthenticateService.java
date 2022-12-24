package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.UnableToFindCredentialsException;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorCredentialValueReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorCredentialReaderManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorCredentialValueReaderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class AuthenticateService {

    @Autowired
    public VendorCredentialReaderManager vendorCredentialReaderManager;

    @Autowired
    public VendorCredentialValueReaderManager vendorCredentialValueReaderManager;

    @Autowired
    private VendorPlayerAuthenticationRepository vendorPlayerAuthenticationRepository;

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
