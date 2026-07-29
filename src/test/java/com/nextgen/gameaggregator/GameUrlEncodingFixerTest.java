package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.operator.game.url.GameUrlEncodingFixer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GameUrlEncodingFixerTest {

    private static final Integer TEST_VENDOR_ID = 45;
    private static final Integer OTHER_VENDOR_ID = 99;

    private final GameUrlEncodingFixer fixer = new GameUrlEncodingFixer();

    /**
     * Configures the fixer as if {@code game-url.encoding-fix.enabled-vendors}
     * were set to {@code enabledVendorsConfig}, then returns it for chaining.
     * Mirrors what {@code @PostConstruct init()} does, since a plain
     * {@code new GameUrlEncodingFixer()} never runs Spring's lifecycle hook.
     */
    private GameUrlEncodingFixer configured(String enabledVendorsConfig) {
        ReflectionTestUtils.setField(fixer, "enabledVendorsRaw", enabledVendorsConfig);
        ReflectionTestUtils.invokeMethod(fixer, "init");
        return fixer;
    }

    private GameUrlEncodingFixer enabled() {
        return configured(String.valueOf(TEST_VENDOR_ID));
    }

    @Test
    void doesNotTouchSignedBase64Param_containingPercentEncodedEquals() {
        String url = "https://vendor.com/launch?sign=YWJjZA%3D%3D&homepage=https://ga.com/return%3FgameId%3D123";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("sign=YWJjZA%3D%3D");
    }

    @Test
    void doesNotTouchTokenParam_containingPercentEncodedPlus() {
        String url = "https://vendor.com/launch?token=a%2Bb&homepage=https://ga.com/return%3FgameId%3D123";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("token=a%2Bb");
    }

    @Test
    void doesNotTouchRawPlus_inUnrelatedParam() {
        String url = "https://vendor.com/launch?nickname=john+doe&homepage=https://ga.com/return%3FgameId%3D123";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("nickname=john+doe");
    }

    @Test
    void repairsOverEncodedEqualsInsideEmbeddedHomepageParam() {
        String url = "https://casino-gateway.petros04.com/launch?token=01KT90CHT2YEMRSPME&cid=GPK&brand=GPK" +
                "&homepage=https://clientsapi22.ibbf55-resources.com/casino/getReturnPage?gameId%3D260427" +
                "&launchAlias=launch_slots_joicy_coins&lang=en&nolobby=1&social=0&username=38506559";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("homepage=");
    }

    @Test
    void doesNotCorruptTopLevelAmpersand_fromEmbeddedEncodedAmpersand() {
        // This passes securely because we never decode the full top-level query string as a whole.
        // The TOP_LEVEL_QUERY_PARAM regex splits parameters by literal '&' on the raw un-decoded text.
        // Therefore, %26 remains textually distinct from structural separators.
        String url = "https://vendor.com/launch?homepage=https://ga.com/cb?a%3D1%26b%3D2&token=abc";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("&token=abc");
    }

    @Test
    void featureFlagDisabled_returnsOriginalUnchanged() {
        String url = "https://vendor.com/launch?homepage=https://ga.com/return%3FgameId%3D123";

        String result = fixer.normalize(url, TEST_VENDOR_ID);

        assertThat(result).isEqualTo(url);
    }

    @Test
    void vendorNotInAllowlist_returnsOriginalUnchanged() {
        String url = "https://vendor.com/launch?homepage=https://ga.com/return%3FgameId%3D123";

        String result = enabled().normalize(url, OTHER_VENDOR_ID);

        assertThat(result).isEqualTo(url);
    }

    @Test
    void wildcardEnablesEveryVendor() {
        String url = "https://vendor.com/launch?homepage=https://ga.com/return%3FgameId%3D123";

        String result = configured("*").normalize(url, OTHER_VENDOR_ID);

        // any vendorId is accepted under "*" - same repaired shape as the allowlisted-vendor case
        assertThat(result).isEqualTo(configured("*").normalize(url, TEST_VENDOR_ID));
        assertThat(result).isNotEqualTo(url);
    }

    @Test
    void commaSeparatedList_enablesOnlyListedVendors() {
        GameUrlEncodingFixer configuredFixer = configured("45,85");
        String url = "https://vendor.com/launch?homepage=https://ga.com/return%3FgameId%3D123";

        assertThat(configuredFixer.normalize(url, 45)).contains("homepage=");
        assertThat(configuredFixer.normalize(url, 85)).contains("homepage=");
        assertThat(configuredFixer.normalize(url, 99)).isEqualTo(url);
    }

    @Test
    void nullVendorId_neverEnabled_evenWithWildcardConfig_isStillDisabledWithoutWildcard() {
        String url = "https://vendor.com/launch?homepage=https://ga.com/return%3FgameId%3D123";

        String result = configured("45").normalize(url, null);

        assertThat(result).isEqualTo(url);
    }

    @Test
    void nullOrBlankUrl_returnsAsIs() {
        assertThat(enabled().normalize(null, TEST_VENDOR_ID)).isNull();
        assertThat(enabled().normalize("", TEST_VENDOR_ID)).isEmpty();
    }

    @Test
    void percentDecodeDoesNotTurnPlusIntoSpace_inNestedParam() {
        // raw '+' must survive decode/encode round-trip
        String url = "https://vendor.com/launch?token=a+b&homepage=https://ga.com/path%2Bmore";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        // The %2B within whitelisted homepage must resolve to a literal '+' without being transformed into a space,
        // and re-encoding it must produce %2B rather than %20 or a literal space.
        assertThat(result).contains("homepage=https%3A%2F%2Fga.com%2Fpath%2Bmore");
        // Non-whitelisted token parameter must stay identical down to its literal '+'
        assertThat(result).contains("token=a+b");
    }

    @Test
    void signParam_base64PaddingSurvivesUntouched() {
        //  sign parameter is not whitelisted and must remain unperturbed byte-for-byte
        String url = "https://vendor.com/launch?sign=YWJjZA%3D%3D&homepage=https://ga.com/return%3FgameId%3D123";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("sign=YWJjZA%3D%3D");
    }

    @Test
    void valuelessQueryParam_isPreservedVerbatim() {
        // a bare flag with no '=' must not be silently dropped
        // by the rebuild loop's assumption that matches are always separated by a single '&'.
        String url = "https://v.com/g?flag&homepage=https://x.com/a&t=1";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).contains("flag&");
        assertThat(result).contains("t=1");
    }

    @Test
    void urlFragment_isNotAbsorbedIntoWhitelistedParamValue() {
        // fragment after the query string must survive untouched,
        // not get pulled into the last whitelisted param's value and percent-encoded as %23.
        String url = "https://v.com/g?t=1&homepage=https://x.com/cb#top";

        String result = enabled().normalize(url, TEST_VENDOR_ID);

        assertThat(result).endsWith("#top");
        assertThat(result).doesNotContain("%23");
    }
}
