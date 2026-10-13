package com.nextgen.gameaggregator.vendor.jdb.validator;

import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs the bean-validation constraints declared on the JDB debit/credit wire DTOs.
 *
 * <p><b>Why JDB needs this at all.</b> Every other vendor on the new framework is a real
 * {@code @RestController} taking {@code @Valid @RequestBody} (crystal, aviatorstudio, cosmoplay,
 * digitain, …), so Spring MVC binds and validates for them and none of them calls
 * {@code ValidationUtils} directly. JDB cannot: its payload is encrypted inside {@code x} and
 * posted as form data ({@code dc} + {@code x}), so it has to be decrypted with per-vendor-line
 * credentials before it is even JSON. {@code GeneralAction} therefore hand-deserializes it, MVC
 * never sees a bindable body, and a method-parameter {@code @Valid} would have nothing to hook
 * onto. Add {@code AbstractProcessorController.processRequest} going deserialize →
 * {@code mapToInternal} with no validation step, and every
 * {@code @NotNull}/{@code @NotBlank}/{@code @Pattern}/{@code @Positive} on
 * {@code DebitActionDto}, {@code SettleActionDto} and {@code RefundActionDto} was declared but
 * never executed — malformed requests were accepted with {@code 0000}. This class is that missing
 * step. If JDB ever moves to a plaintext endpoint, delete it and annotate the parameter instead.
 *
 * <p><b>Wire codes.</b> {@code ValidationUtils.validateRequest} records a violation's message only
 * when it is a literal (a constraint declared with {@code message = ResponseCode.X}), and
 * {@code null} for the interpolated default messages. So a constraint that names its code —
 * {@code @Positive(message = PARAMETER_CANNOT_BE_NEGATIVE)} → {@code 8003},
 * {@code @Pattern(..., message = WRONG_DATE_FORMAT)} → {@code 8005} — surfaces that code, and
 * everything else falls back to {@code 8000}. A custom code always wins over a default one.
 *
 * <p><b>Several custom codes at once.</b> The winner comes from {@code HashMap} iteration order,
 * which makes it <i>stable for a given payload</i> — the same request yields the same code on
 * every run — but <i>unspecified by design</i>: it is not predictable from reading this code and
 * may shift if field names change or the map resizes. Do not write assertions that depend on it.
 * Picking one deliberately was considered and rejected: the legacy {@code BetService} selects from
 * the same map the same way, so defining a precedence only here would make actions 9/10/11 answer
 * a different code than action 8 for the same payload.
 *
 * <p><b>Divergence from the house pattern, on purpose.</b> {@code RequestValidationExceptionHandler}
 * answers a validation failure with one generic code from {@code onInvalidRequestError} plus an
 * optional {@code fieldErrors} list, rather than choosing a per-field code. JDB returns the
 * specific code instead, because the vendor's spec demands it ({@code 8003} for negatives,
 * {@code 8005} for bad dates) and because JDB's wire response is
 * {@code {status, balance, err_text}} — a single status field with nowhere to put a list.
 *
 * <p>Legacy actions (4 / 6 / 8) are untouched — they keep their own DTOs and their own
 * in-service {@code doValidation} call.
 */
public final class JdbRequestValidator {

    private JdbRequestValidator() {
    }

    /**
     * @return an error {@link CommonVo} carrying the violated constraint's wire code, or
     *         {@link Optional#empty()} when the request satisfies its constraints.
     */
    public static Optional<CommonVo> validate(Object request) {
        String code = firstViolationCode(request);
        if (code == null) {
            return Optional.empty();
        }
        CommonVo vo = new CommonVo();
        vo.setErrorResponseCode(code);
        return Optional.of(vo);
    }

    /**
     * @return the JDB wire code for the first violated constraint, or {@code null} when valid.
     */
    public static String firstViolationCode(Object request) {
        try {
            ValidationUtils.validateRequest(request);
            return null;
        } catch (InvalidRequestException ex) {
            Map<String, String> violations = ex.getValidation();
            if (violations == null || violations.isEmpty()) {
                return ResponseCode.INVALID_REQUEST_PARAMETER;
            }
            // Values are null for default messages; a constraint that names its own code wins.
            return violations.values().stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(ResponseCode.INVALID_REQUEST_PARAMETER);
        }
    }
}
