package com.nextgen.gameaggregator.vendor.lucky365.validator;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.lucky365.constant.Credentials;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * OAS-4849 / GA-14626 — Lucky365 sends the {@code LoginId} back upper-cased in callbacks, but the
 * stored {@code vendor_players.username} is lower-case on a case-sensitive column. The validator
 * must look the player up with the lower-cased LoginId, yet sign with the RAW LoginId (the vendor
 * computes its MD5 over the exact case it sends).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Lucky365SignatureValidatorTest {

    private static final String STORED_USERNAME = "2f8ka";   // as generated/stored (lower-case)
    private static final String VENDOR_LOGIN_ID = "2F8KA";   // as Lucky365 sends it back (upper-case)
    private static final int VENDOR_LINE_ID = 42;

    private static final String ID = "abc123";
    private static final String METHOD = "Bet";
    private static final String SN = "SN123";
    private static final String SECRET = "secret999";

    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorLineService vendorLineService;

    private Lucky365SignatureValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new Lucky365SignatureValidator(vendorPlayerDataService, vendorLineService);

        VendorPlayer player = new VendorPlayer();
        player.setVendorLineId(VENDOR_LINE_ID);

        // Case-sensitive lookup: only the lower-cased username resolves; the raw upper-case misses
        // (this is exactly what happens on the case-sensitive `latin1_general_cs` column on a cache miss).
        when(vendorPlayerDataService.getByUsername(STORED_USERNAME)).thenReturn(player);
        when(vendorPlayerDataService.getByUsername(VENDOR_LOGIN_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VENDOR_LOGIN_ID));

        when(vendorLineService.getCredentialValueByName(VENDOR_LINE_ID, Credentials.SECRET_KEY)).thenReturn(SECRET);
        when(vendorLineService.getCredentialValueByName(VENDOR_LINE_ID, Credentials.SERIAL_NUM)).thenReturn(SN);
    }

    private String rawBody(String loginId, String signature) {
        return "[{\"Signature\":\"" + signature + "\",\"LoginId\":\"" + loginId
                + "\",\"ID\":\"" + ID + "\",\"Method\":\"" + METHOD + "\"}]";
    }

    private static String sign(String loginId) {
        return DigestUtils.md5Hex(ID + METHOD + SN + loginId + SECRET);
    }

    @Test
    @DisplayName("upper-cased LoginId: looks up lower-cased, signs with raw case → passes (regression guard for OAS-4849)")
    void upperCaseLoginId_resolvesPlayerAndValidates() {
        // Vendor signs over the exact (upper-case) value it sends.
        String vendorSignature = sign(VENDOR_LOGIN_ID);
        String body = rawBody(VENDOR_LOGIN_ID, vendorSignature);

        ValidationResult result = validator.validate(new MockHttpServletRequest(), null, body);

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("signature is computed over the RAW LoginId, not the normalised one → wrong-case signature is rejected")
    void signatureUsesRawLoginId_notNormalised() {
        // A signature computed over the LOWER-cased value must NOT be accepted for an upper-case LoginId,
        // otherwise someone lower-casing the hash input would silently break real (upper-case-signed) traffic.
        String signatureOverLowerCase = sign(STORED_USERNAME);
        String body = rawBody(VENDOR_LOGIN_ID, signatureOverLowerCase);

        assertThatThrownBy(() -> validator.validate(new MockHttpServletRequest(), null, body))
                .isInstanceOf(SignatureValidationException.class);
    }

    @Test
    @DisplayName("already lower-case LoginId still validates")
    void lowerCaseLoginId_validates() {
        String vendorSignature = sign(STORED_USERNAME);
        String body = rawBody(STORED_USERNAME, vendorSignature);

        ValidationResult result = validator.validate(new MockHttpServletRequest(), null, body);

        assertThat(result.valid()).isTrue();
    }
}
