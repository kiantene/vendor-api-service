package com.nextgen.gameaggregator.operator.game.url;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fix for vendors that over-encode characters (e.g. "=" -> "%3D") inside a URL
 * parameter value that itself contains a nested callback URL (e.g.
 * lobbyUrl/homepage embedded as a query param).
 * <p>
 * IMPORTANT — per review feedback: we NEVER decode a whole query string /
 * multi-param container as one blob. java.net.URLDecoder is a FORM decoder
 * (application/x-www-form-urlencoded): it turns literal '+' into a space,
 * which corrupts any real URL that legitimately contains a '+'. We also
 * never decode-then-reparse an entire "outer" URL, because %26 inside a
 * nested value would then be misread as a top-level '&' — the decode step
 * destroys the distinction between "structure" and "data" irreversibly.
 * <p>
 * Instead: we only ever decode/re-encode ONE isolated value at a time — a
 * single whitelisted query parameter's value — using a percent-decode that
 * leaves '+' untouched, and a matching percent-encode that never emits '+'
 * for spaces.
 */
@Slf4j
@Component
public class GameUrlEncodingFixer {

    private static final Set<String> NESTED_URL_PARAM_NAMES = Set.of("homepage", "lobbyUrl", "lobby_url", "return_url", "leaveUrl");

    private static final Pattern TOP_LEVEL_QUERY_PARAM =
            Pattern.compile("([^&=?]+)=([^&]*)");

    // "" (default) = disabled for everyone; "45,85" = only those vendorIds;
    // "*" = every vendor (equivalent to the old global enabled=true flag).
    @Value("${game-url.encoding-fix.enabled-vendors:}")
    private String enabledVendorsRaw;

    private boolean allVendorsEnabled;
    // Defaults to empty (not null) so the fixer is safely "disabled for everyone"
    // even before init() has run (e.g. a plain `new GameUrlEncodingFixer()` in a
    // test, where Spring's @PostConstruct lifecycle never fires).
    private Set<Integer> enabledVendorIds = Set.of();

    @PostConstruct
    void init() {
        String raw = enabledVendorsRaw == null ? "" : enabledVendorsRaw.trim();
        allVendorsEnabled = raw.equals("*");
        enabledVendorIds = allVendorsEnabled ? Set.of() : Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private boolean isEnabledForVendor(Integer vendorId) {
        return allVendorsEnabled || (vendorId != null && enabledVendorIds.contains(vendorId));
    }

    /**
     * Normalizes a vendor-returned gameUrl: repairs any whitelisted nested-URL
     * query parameter whose value was over-encoded by the vendor. Does not
     * touch any other parameter's value.
     */
    public String normalize(String gameUrl, Integer vendorId) {
        if (!isEnabledForVendor(vendorId)) {
            return gameUrl;
        }
        if (gameUrl == null || gameUrl.isBlank()) {
            return gameUrl;
        }

        try {
            int queryStart = gameUrl.indexOf('?');
            if (queryStart < 0) {
                return gameUrl;
            }

            String base = gameUrl.substring(0, queryStart);
            String afterQueryMark = gameUrl.substring(queryStart + 1);

            // Split off the URL fragment (if any) before parsing the query string,
            // so it is never absorbed into the last parameter's value and
            // incorrectly percent-encoded as part of a nested URL repair.
            int fragmentStart = afterQueryMark.indexOf('#');
            String query = fragmentStart >= 0 ? afterQueryMark.substring(0, fragmentStart) : afterQueryMark;
            String fragment = fragmentStart >= 0 ? afterQueryMark.substring(fragmentStart) : "";

            StringBuilder rebuiltQuery = new StringBuilder();
            Matcher matcher = TOP_LEVEL_QUERY_PARAM.matcher(query);
            int lastEnd = 0;

            while (matcher.find()) {
                // Copy verbatim any text between the previous match and this one
                // (e.g. a valueless flag like "flag" with no '='), instead of
                // synthesizing '&' separators that silently drop such segments.
                rebuiltQuery.append(query, lastEnd, matcher.start());

                String paramName = matcher.group(1);
                String paramValue = matcher.group(2);

                if (NESTED_URL_PARAM_NAMES.contains(paramName)) {
                    String repairedValue = repairNestedUrlValue(paramValue);
                    rebuiltQuery.append(paramName).append('=').append(repairedValue);
                } else {
                    rebuiltQuery.append(paramName).append('=').append(paramValue);
                }

                lastEnd = matcher.end();
            }

            if (lastEnd < query.length()) {
                rebuiltQuery.append(query.substring(lastEnd));
            }

            return base + "?" + rebuiltQuery + fragment;

        } catch (Exception e) {
            log.warn("Failed to normalize gameUrl encoding, returning original. gameUrl={}", gameUrl, e);
            return gameUrl;
        }
    }

    private String repairNestedUrlValue(String rawValue) {
        String decoded = percentDecode(rawValue);
        try {
            new URI(decoded);
        } catch (Exception e) {
            log.warn("Nested param value did not decode into a valid URI, leaving original. value={}", rawValue);
            return rawValue;
        }
        return percentEncode(decoded);
    }

    /**
     * Decodes %XX sequences only. Unlike java.net.URLDecoder, this NEVER treats
     * a literal '+' as a space — '+' is a valid, unreserved character in a URL
     * and must survive decode unchanged.
     */
    private static String percentDecode(String value) {
        StringBuilder result = new StringBuilder();
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int i = 0;
        int len = value.length();

        while (i < len) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < len && isHex(value.charAt(i + 1)) && isHex(value.charAt(i + 2))) {
                int code = Integer.parseInt(value.substring(i + 1, i + 3), 16);
                byteBuffer.write(code);
                i += 3;
            } else {
                flushBytes(byteBuffer, result);
                result.append(c);
                i++;
            }
        }
        flushBytes(byteBuffer, result);
        return result.toString();
    }

    /**
     * Encodes a value for safe use as a URL query parameter value. Unlike
     * java.net.URLEncoder alone, this does NOT emit '+' for spaces — spaces
     * are encoded as %20, matching what percentDecode expects on a round-trip.
     */
    private static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void flushBytes(ByteArrayOutputStream buffer, StringBuilder result) {
        if (buffer.size() > 0) {
            result.append(buffer.toString(StandardCharsets.UTF_8));
            buffer.reset();
        }
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}