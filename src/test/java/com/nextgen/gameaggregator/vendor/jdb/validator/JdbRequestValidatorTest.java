package com.nextgen.gameaggregator.vendor.jdb.validator;

import com.nextgen.gameaggregator.vendor.jdb.api.credit.RefundActionDto;
import com.nextgen.gameaggregator.vendor.jdb.api.credit.SettleActionDto;
import com.nextgen.gameaggregator.vendor.jdb.api.debit.DebitActionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each case names the ticket whose reported symptom it locks in. Every one of these requests was
 * previously answered {@code 0000} (or HTTP 500) because the DTO constraints never executed.
 */
class JdbRequestValidatorTest {

    private static DebitActionDto debit() {
        DebitActionDto d = new DebitActionDto();
        d.setAction("9");
        d.setTs(1789545096621L);
        d.setTransferId("40581947");
        d.setUid("tm63tllpc");
        d.setCurrency("PP");
        d.setAmount(new BigDecimal("20"));
        d.setGameRoundSeqNo(8009320445L);
        d.setGType("18");
        d.setMType("18026");
        return d;
    }

    private static SettleActionDto settle() {
        SettleActionDto c = new SettleActionDto();
        c.setAction("10");
        c.setTs(1789545097067L);
        c.setTransferId("40581948");
        c.setUid("tm63tllpc");
        c.setCurrency("PP");
        c.setAmount(new BigDecimal("40"));
        c.setRefTransferIds(List.of(40581947L));
        c.setGameRoundSeqNo(8009320445L);
        c.setHistoryId("5251023779659");
        c.setGType("18");
        c.setMType("18026");
        c.setBet(new BigDecimal("20"));
        c.setValidBet(new BigDecimal("20"));
        c.setWin(new BigDecimal("40"));
        c.setNetWin(new BigDecimal("20"));
        return c;
    }

    private static RefundActionDto refund() {
        RefundActionDto c = new RefundActionDto();
        c.setAction("11");
        c.setTs(1789545097067L);
        c.setTransferId("40581949");
        c.setUid("tm63tllpc");
        c.setCurrency("PP");
        c.setAmount(new BigDecimal("20"));
        c.setRefTransferIds(List.of(40581947L));
        c.setGameRoundSeqNo(8009320445L);
        c.setGType("18");
        c.setMType("18026");
        return c;
    }

    @Test
    @DisplayName("a well-formed request of each kind passes")
    void validRequests_produceNoRejection() {
        assertNull(JdbRequestValidator.firstViolationCode(debit()));
        assertNull(JdbRequestValidator.firstViolationCode(settle()));
        assertNull(JdbRequestValidator.firstViolationCode(refund()));
        assertTrue(JdbRequestValidator.validate(debit()).isEmpty());
    }

    @Nested
    @DisplayName("OVI-3183 — gType / mType")
    class GameType {
        @Test void gTypeNotSet_is8000() {
            DebitActionDto d = debit(); d.setGType(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void mTypeNotSet_is8000() {
            DebitActionDto d = debit(); d.setMType(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void gTypeBlank_is8000() {
            DebitActionDto d = debit(); d.setGType("");
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void gTypeNotSetOnSettle_is8000() {
            SettleActionDto c = settle(); c.setGType(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void mTypeNotSetOnRefund_is8000() {
            RefundActionDto r = refund(); r.setMType(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(r));
        }
        @Test void nonNumeric_is8000() {
            DebitActionDto d = debit(); d.setGType("abc");
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
    }

    @Nested
    @DisplayName("OVI-3178 — ts")
    class Ts {
        @Test void tsNotSet_is8000() {
            DebitActionDto d = debit(); d.setTs(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void tsNotSetOnSettle_is8000() {
            SettleActionDto c = settle(); c.setTs(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void tsNotSetOnRefund_is8000() {
            RefundActionDto c = refund(); c.setTs(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
    }

    @Nested
    @DisplayName("OVI-3179 — transferId")
    class TransferId {
        @Test void surroundingSpaces_is8000() {
            DebitActionDto d = debit(); d.setTransferId(" 2995714945 ");
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void blank_is8000() {
            DebitActionDto d = debit(); d.setTransferId("");
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
    }

    @Nested
    @DisplayName("OVI-2162 — amount")
    class Amount {
        @Test void notSet_is8000() {
            DebitActionDto d = debit(); d.setAmount(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void negative_is8003() {
            DebitActionDto d = debit(); d.setAmount(new BigDecimal("-50"));
            assertEquals("8003", JdbRequestValidator.firstViolationCode(d),
                    "@Positive(message = PARAMETER_CANNOT_BE_NEGATIVE) must surface as 8003, not 8000");
        }
        @Test void sixDecimals_isAccepted() {
            DebitActionDto d = debit(); d.setAmount(new BigDecimal("20.123456"));
            assertNull(JdbRequestValidator.firstViolationCode(d));
        }
        @Test void nineDecimals_is8000() {
            // [102069] / [102102]: answered 0000 before, with the 9-decimal amount applied.
            DebitActionDto d = debit(); d.setAmount(new BigDecimal("20.123456789"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void sevenDecimals_is8000() {
            RefundActionDto r = refund(); r.setAmount(new BigDecimal("20.1234567"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(r));
        }
        @Test void decimalPlacesCountAsWritten_includingTrailingZeros() {
            // @Digits reads the BigDecimal's scale, so 20.500000000 is 9 decimals even though it
            // equals 20.5. Deliberate: JDB's limit is 6 written decimals, and more is malformed.
            SettleActionDto c = settle(); c.setAmount(new BigDecimal("20.500000000"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void moreThan20IntegerDigits_is8000() {
            // Was 9999 from the operator (SC_UNKNOWN_ERROR); now rejected before any call.
            DebitActionDto d = debit(); d.setAmount(new BigDecimal("123456789012345678901"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void negativeWithTooManyDecimals_isStill8003() {
            DebitActionDto d = debit(); d.setAmount(new BigDecimal("-20.123456789"));
            assertEquals("8003", JdbRequestValidator.firstViolationCode(d));
        }
    }

    @Nested
    @DisplayName("OVI-2291 — bet / win / validBet / netWin")
    class CreditAmounts {
        @Test void betNotSet_is8000() {
            SettleActionDto c = settle(); c.setBet(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void winNotSet_is8000() {
            SettleActionDto c = settle(); c.setWin(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void netWinNotSet_is8000() {
            SettleActionDto c = settle(); c.setNetWin(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void validBetNegative_is8003() {
            SettleActionDto c = settle(); c.setValidBet(new BigDecimal("-1"));
            assertEquals("8003", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void betNegative_is8003() {
            SettleActionDto c = settle(); c.setBet(new BigDecimal("-1"));
            assertEquals("8003", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void betNineDecimals_is8000() {
            // [102083]
            SettleActionDto c = settle(); c.setBet(new BigDecimal("20.123456789"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void winNineDecimals_is8000() {
            // [102084]
            SettleActionDto c = settle(); c.setWin(new BigDecimal("40.123456789"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void validBetAndNetWinNineDecimals_are8000() {
            SettleActionDto v = settle(); v.setValidBet(new BigDecimal("20.123456789"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(v));
            SettleActionDto n = settle(); n.setNetWin(new BigDecimal("-20.123456789"));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(n),
                    "netWin may be negative, so its only possible violation here is the precision");
        }
        @Test void sixDecimalsOnEveryCreditAmount_isAccepted() {
            SettleActionDto c = settle();
            c.setBet(new BigDecimal("20.123456"));
            c.setValidBet(new BigDecimal("20.123456"));
            c.setWin(new BigDecimal("40.123456"));
            c.setNetWin(new BigDecimal("20.000000"));
            assertNull(JdbRequestValidator.firstViolationCode(c));
        }
    }

    @Nested
    @DisplayName("OVI-2164 — gameDate (format only; see gameDateNotSet)")
    class GameDate {
        @Test void reorderedDate_is8005() {
            SettleActionDto c = settle(); c.setGameDate("2026-21-09 05:33:58");
            assertEquals("8005", JdbRequestValidator.firstViolationCode(c),
                    "@Pattern(message = WRONG_DATE_FORMAT) must surface as 8005");
        }
        @Test void shortenedDate_is8005() {
            SettleActionDto c = settle(); c.setGameDate("21-09-2026");
            assertEquals("8005", JdbRequestValidator.firstViolationCode(c));
        }
        // OVI-3191: a malformed date used to fail @Size (8000) AND @Pattern (8005) on the same field,
        // and which one survived varied per request. Each check validates a fresh request many
        // times and requires the same code every time.
        private void assertAlways(String expected, java.util.function.Supplier<SettleActionDto> request) {
            for (int i = 0; i < 50; i++) {
                assertEquals(expected, JdbRequestValidator.firstViolationCode(request.get()),
                        "attempt " + i + " answered a different code");
            }
        }
        @Test void reportDateWithATime_isAlways8005() {
            assertAlways("8005", () -> { SettleActionDto c = settle(); c.setReportDate("15-01-2026 14:18:57"); return c; });
        }
        @Test void gameDateTooLong_isAlways8005() {
            assertAlways("8005", () -> { SettleActionDto c = settle(); c.setGameDate("24-09-2026 01:49:28.123"); return c; });
        }
        @Test void lastModifyTimeTooLong_isAlways8005() {
            assertAlways("8005", () -> { SettleActionDto c = settle(); c.setLastModifyTime("15-01-2026 14:18:57.000"); return c; });
        }
        @Test void validReportDate_withNullLastModifyTimeAndGameDate_isAccepted() {
            // What QA's null tests meant to check: with a well-formed reportDate, both are optional.
            SettleActionDto c = settle();
            c.setReportDate("24-09-2026");
            c.setLastModifyTime(null);
            c.setGameDate(null);
            assertNull(JdbRequestValidator.firstViolationCode(c));
        }
        @Test
        @DisplayName("NOT closed by this change: gameDate absent still passes — no @NotBlank exists")
        void gameDateNotSet_stillPasses() {
            SettleActionDto c = settle(); c.setGameDate(null);
            assertNull(JdbRequestValidator.firstViolationCode(c),
                    "OVI-2164 also asks for 8000 when gameDate is absent. Neither the legacy nor the "
                            + "new DTO declares @NotBlank on gameDate, so enabling validation cannot "
                            + "produce that. Adding the constraint is a separate, deliberate decision.");
        }
    }

    @Nested
    @DisplayName("live defect seen in QA — currency omitted")
    class Currency {
        @Test void fourCharCurrency_isAccepted() {
            // JDB publishes 4-char codes: IRRK, KHRO, KRWK, VNDC, DOGE, mBTC, USDC, POGC, POSC.
            DebitActionDto d = debit(); d.setCurrency("KRWK");
            assertNull(JdbRequestValidator.firstViolationCode(d));
        }
        @Test void fourCharCurrencyOnSettle_isAccepted() {
            SettleActionDto c = settle(); c.setCurrency("USDC");
            assertNull(JdbRequestValidator.firstViolationCode(c));
        }
        @Test void fourCharCurrencyOnRefund_isAccepted() {
            RefundActionDto r = refund(); r.setCurrency("VNDC");
            assertNull(JdbRequestValidator.firstViolationCode(r));
        }
        @Test void lowerCaseFourCharCurrency_isAccepted() {
            // mBTC is published with a lower-case m; the constraint must not assume upper case.
            DebitActionDto d = debit(); d.setCurrency("mBTC");
            assertNull(JdbRequestValidator.firstViolationCode(d));
        }
        @Test void fiveCharCurrency_is8000() {
            DebitActionDto d = debit(); d.setCurrency("TOOLONG");
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d));
        }
        @Test void nullCurrency_is8000() {
            DebitActionDto d = debit(); d.setCurrency(null);
            assertEquals("8000", JdbRequestValidator.firstViolationCode(d),
                    "a debit with no currency was accepted 0000 in QA and only got its currency "
                            + "from the session-backfill fallback");
        }
    }

    @Nested
    @DisplayName("container-element constraints are honoured")
    class RefTransferIds {
        @Test void nullElement_is8000() {
            SettleActionDto c = settle(); c.setRefTransferIds(Arrays.asList((Long) null));
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
        @Test void emptyList_is8000() {
            SettleActionDto c = settle(); c.setRefTransferIds(List.of());
            assertEquals("8000", JdbRequestValidator.firstViolationCode(c));
        }
    }

    @Test
    @DisplayName("validate() returns a CommonVo carrying the code and its JDB err_text")
    void validate_buildsErrorVo() {
        DebitActionDto d = debit();
        d.setAmount(new BigDecimal("-1"));
        var vo = JdbRequestValidator.validate(d).orElseThrow();
        assertEquals("8003", vo.getStatus());
        assertEquals("The parameter cannot be negative.", vo.getErr_text());
    }
}
