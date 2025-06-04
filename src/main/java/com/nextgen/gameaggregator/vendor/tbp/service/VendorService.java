package com.nextgen.gameaggregator.vendor.tbp.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private final VendorLineService vendorLineService;

    public VendorService(VendorLineService vendorLineService) {
        this.vendorLineService = vendorLineService;
    }

    public void validate(String usernameDto, String passwordDto, String playerIdDto, GameSession gameSession)
            throws AuthenticationException, CredentialNotFoundException {

        // 1. Verify Username
        String username = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        ValidationUtils.isEquals(username, usernameDto, AuthenticationException::new);

        // 2. Verify Password
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.TOKEN);
        ValidationUtils.isEquals(password, passwordDto, AuthenticationException::new);

        // 3. Verify PlayerId
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), playerIdDto, AuthenticationException::new);
    }
}
