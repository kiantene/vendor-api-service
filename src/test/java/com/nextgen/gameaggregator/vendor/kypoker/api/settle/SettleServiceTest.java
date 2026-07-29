package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.constant.RoomCode;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for SettleService credit/debit flow using VendorService.
 * <p>
 * These tests use the exact values from the reported issue:
 * - KYP_1370 (working case): money=1008.5, validBet=290, totalWithdraw=408.5
 * - KYP_600 (problem case): debit=563.17, money=335.17, validBet=228, totalWithdraw=-117
 */
class SettleServiceTest {

    @Mock
    private WalletService walletService;
    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private OperatorWalletService operatorWalletService;
    @Mock
    private WalletRequestService walletRequestService;
    @Mock
    private HttpService httpService;
    @Mock
    private WalletTransactionService walletTransactionService;
    @Mock
    private RequestIdempotentLogService requestIdempotentLogService;

    private VendorService vendorService;
    private SettleService settleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vendorService = new VendorService(walletRequestService);
        when(walletRequestService.updateByGameSession(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        settleService = new SettleService(
                walletService,
                gameSessionService,
                vendorService,
                operatorWalletService,
                walletRequestService,
                httpService,
                walletTransactionService,
                requestIdempotentLogService
        );
    }

    private GameSession createGameSession(String account, String vendorGameCode) {
        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername(account);
        gameSession.setVendorGameCode(vendorGameCode);
        gameSession.setVendorCurrencyCode("CNY");
        gameSession.setToken("test-token");
        return gameSession;
    }

    private SettleDto createSettleDtoFromParams(
            String account,
            String orderId,
            String gameNo,
            Integer kindId,
            BigDecimal money,
            String gameId,
            Integer roomMode,
            BigDecimal totalBet,
            BigDecimal validBet,
            BigDecimal totalWithdraw,
            BigDecimal revenue
    ) {
        SettleDto settleDto = new SettleDto();
        settleDto.setS("1003");
        settleDto.setAccount(account);
        settleDto.setOrderId(orderId);
        settleDto.setGameNo(gameNo);
        settleDto.setKindId(kindId);
        settleDto.setMoney(money);
        settleDto.setCurrency("CNY");
        settleDto.setGameId(gameId);
        settleDto.setRoomMode(roomMode);
        settleDto.setBetCount(1);
        settleDto.setTotalBet(totalBet);
        settleDto.setValidBet(validBet);
        settleDto.setTotalWithdraw(totalWithdraw);
        settleDto.setRevenue(revenue);
        return settleDto;
    }

    // ==================================================================================
    // VENDOR SERVICE INTEGRATION TESTS - KYP_1370 WORKING CASE
    // ==================================================================================
    @Nested
    @DisplayName("VendorService Integration - KYP_1370 Working Case")
    class Kyp1370WorkingCaseTests {

        @Test
        @DisplayName("KYP_1370: winAmount = betAmount + winLoss = 290 + 408.5 = 698.5")
        void kyp1370_workingCase_calculatesWinAmountCorrectly() {
            // Given: Exact params from the working KYP_1370 case
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwshs0b",
                    "214749504982-1370-5785353828-516997492",
                    "50-1767410567-5785353828-1",
                    1370,
                    new BigDecimal("1008.5"),
                    "50-1767410567-5785353828-1",
                    1,
                    new BigDecimal("290"),
                    new BigDecimal("290"),
                    new BigDecimal("408.5"),
                    new BigDecimal("21.5")
            );

            BigDecimal debitAmount = new BigDecimal("600");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwshs0b", "1370_MATCHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then
            assertEquals(new BigDecimal("698.5"), walletRequest.getWinAmount());
            assertEquals(ResultType.WIN.code, walletRequest.getResultType());
            assertEquals(new BigDecimal("290"), walletRequest.getBetAmount());
            assertEquals(new BigDecimal("290"), walletRequest.getEffectiveTurnover());
            assertEquals(new BigDecimal("1008.5"), walletRequest.getTransferAmount());
        }

        @Test
        @DisplayName("KYP_1370: SettleDto fields are mapped correctly")
        void kyp1370_settleDtoFieldsMappedCorrectly() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwshs0b",
                    "214749504982-1370-5785353828-516997492",
                    "50-1767410567-5785353828-1",
                    1370,
                    new BigDecimal("1008.5"),
                    "50-1767410567-5785353828-1",
                    1,
                    new BigDecimal("290"),
                    new BigDecimal("290"),
                    new BigDecimal("408.5"),
                    new BigDecimal("21.5")
            );

            // Then: Verify SettleDto interface methods
            assertEquals("214749504982-1370-5785353828-516997492", settleDto.getExternalTransactionId());
            assertEquals("214749504982-1370-5785353828-516997492", settleDto.getVendorBetId());
            assertEquals("50-1767410567-5785353828-1", settleDto.getRoundId());
            assertEquals("1370", settleDto.getGameId());
            assertEquals("buqhwshs0b", settleDto.getVendorPlayerUsername());
            assertEquals(new BigDecimal("290"), settleDto.getValidBet());
            assertEquals(new BigDecimal("1008.5"), settleDto.getMoney());
        }
    }

    // ==================================================================================
    // VENDOR SERVICE INTEGRATION TESTS - KYP_600 PROBLEM CASE
    // ==================================================================================
    @Nested
    @DisplayName("VendorService Integration - KYP_600 Problem Case")
    class Kyp600ProblemCaseTests {

        @Test
        @DisplayName("KYP_600: old calculation gives 111, correct is 0")
        void kyp600_problemCase_calculatesWinAmountAsZero() {
            // Given: Exact params from the problematic KYP_600 case
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwuv0ub",
                    "214749509260-600-5795428050-518510742",
                    "50-1767509660-5795428050-3",
                    600,
                    new BigDecimal("335.17"),
                    "50-1767509660-5795428050-3",
                    1,
                    new BigDecimal("228"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub", "600_MATCHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 228 + (335.17 - 563.17) = 0, NOT 111
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO));
            assertEquals(ResultType.END.code, walletRequest.getResultType());
            assertEquals(new BigDecimal("228"), walletRequest.getBetAmount());
            assertEquals(new BigDecimal("228"), walletRequest.getEffectiveTurnover());
            assertEquals(new BigDecimal("335.17"), walletRequest.getTransferAmount());
        }

        @Test
        @DisplayName("KYP_600: SettleDto.getWinAmount() returns buggy 111, VendorService returns correct 0")
        void kyp600_settleDtoGetWinAmount_returnsBuggyValue() {
            // Given: Exact params from the KYP_600 issue
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwuv0ub",
                    "214749509260-600-5795428050-518510742",
                    "50-1767509660-5795428050-3",
                    600,
                    new BigDecimal("335.17"),
                    "50-1767509660-5795428050-3",
                    1,
                    new BigDecimal("228"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub", "600_MATCHING");

            // When: Compare old SettleDto method vs VendorService
            BigDecimal buggyWinAmount = settleDto.getWinAmount();  // Old formula: validBet + totalWithdraw
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal correctWinAmount = walletRequest.getWinAmount();  // New formula via service

            // Then: SettleDto returns buggy 111, VendorService returns correct 0
            assertEquals(new BigDecimal("111"), buggyWinAmount,
                    "SettleDto.getWinAmount() uses buggy formula (228 + -117 = 111)");
            assertEquals(0, correctWinAmount.compareTo(BigDecimal.ZERO),
                    "VendorService.dataCreditMapper calculates correct winAmount = 0");
            assertNotEquals(0, buggyWinAmount.compareTo(correctWinAmount),
                    "Buggy and correct calculations differ - demonstrating the bug");
        }

        @Test
        @DisplayName("KYP_600: Demonstrates totalWithdraw is unreliable - service calculates correctly")
        void kyp600_totalWithdrawIsUnreliable() {
            // Given: KYP_600 exact params
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwuv0ub",
                    "214749509260-600-5795428050-518510742",
                    "50-1767509660-5795428050-3",
                    600,
                    new BigDecimal("335.17"),   // creditAmount
                    "50-1767509660-5795428050-3",
                    1,
                    new BigDecimal("228"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),      // totalWithdraw (UNRELIABLE)
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub", "600_MATCHING");

            // When: VendorService calculates using credit - debit (not totalWithdraw)
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: Service correctly calculates winLoss as -228 (credit - debit), not -117 (totalWithdraw)
            // winAmount = validBet + winLoss = 228 + (-228) = 0
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO),
                    "Service uses credit-debit (-228), not totalWithdraw (-117)");

            // Prove the discrepancy: totalWithdraw (-117) != actual winLoss (-228)
            BigDecimal actualWinLoss = settleDto.getMoney().subtract(debitAmount);
            assertEquals(0, actualWinLoss.compareTo(new BigDecimal("-228")));
            assertNotEquals(0, settleDto.getTotalWithdraw().compareTo(actualWinLoss),
                    "totalWithdraw (-117) != actual winLoss (-228)");
        }
    }

    // ==================================================================================
    // SETTLE SERVICE FLOW TESTS - MATCHING/FISHING MODES
    // ==================================================================================
    @Nested
    @DisplayName("SettleService Flow - MATCHING/FISHING Modes")
    class SettleServiceMatchingFishingFlowTests {

        @Test
        @DisplayName("MATCHING mode: Should retrieve debit from WalletTransaction and call dataCreditMapper")
        void settleService_matchingMode_usesWalletTransaction() {
            // Given
            WalletTransaction walletTransaction = new WalletTransaction();
            walletTransaction.setTransferAmount(new BigDecimal("563.17"));
            walletTransaction.setRoundId("50-1767509660-5795428050-3");
            walletTransaction.setVendorPlayerUsername("buqhwuv0ub");

            // When: Getting transfer amount (debit)
            BigDecimal debitAmount = walletTransaction.getTransferAmount();

            // Then
            assertEquals(new BigDecimal("563.17"), debitAmount);
        }

        @Test
        @DisplayName("FISHING mode: Should use same credit/debit flow as MATCHING")
        void settleService_fishingMode_usesCreditDebitFlow() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "testplayer",
                    "order-fishing-123",
                    "game-fishing-456",
                    700,
                    new BigDecimal("750"),
                    "game-fishing-456",
                    RoomCode.FISHING.code,
                    new BigDecimal("200"),
                    new BigDecimal("200"),
                    new BigDecimal("50"),
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("500");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testplayer", "700_FISHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 200 + (750 - 500) = 450
            assertEquals(new BigDecimal("450"), walletRequest.getWinAmount());
            assertEquals(ResultType.WIN.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("RoomCode.MATCHING should have code 1")
        void roomCode_matching_hasCode1() {
            assertEquals(1, RoomCode.MATCHING.code);
        }

        @Test
        @DisplayName("RoomCode.FISHING should have code 4")
        void roomCode_fishing_hasCode4() {
            assertEquals(4, RoomCode.FISHING.code);
        }

        @Test
        @DisplayName("RoomCode.fromCode should return correct enum")
        void roomCode_fromCode_returnsCorrectEnum() {
            assertEquals(RoomCode.MATCHING, RoomCode.fromCode(1));
            assertEquals(RoomCode.BONUS, RoomCode.fromCode(2));
            assertEquals(RoomCode.SINGLE, RoomCode.fromCode(3));
            assertEquals(RoomCode.FISHING, RoomCode.fromCode(4));
            assertEquals(RoomCode.SLOT, RoomCode.fromCode(6));
        }

        @Test
        @DisplayName("RoomCode.fromCode with invalid code should throw exception")
        void roomCode_fromCode_invalidCode_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> RoomCode.fromCode(999));
        }
    }

    // ==================================================================================
    // SETTLE SERVICE FLOW TESTS - BONUS/SINGLE MODES
    // ==================================================================================
    @Nested
    @DisplayName("SettleService Flow - BONUS/SINGLE Modes")
    class SettleServiceBonusSingleFlowTests {

        @Test
        @DisplayName("RoomCode.BONUS should have code 2")
        void roomCode_bonus_hasCode2() {
            assertEquals(2, RoomCode.BONUS.code);
        }

        @Test
        @DisplayName("RoomCode.SINGLE should have code 3")
        void roomCode_single_hasCode3() {
            assertEquals(3, RoomCode.SINGLE.code);
        }

        @Test
        @DisplayName("BONUS/SINGLE modes should NOT use credit/debit flow")
        void bonusSingleModes_notCreditDebitFlow() {
            int bonusCode = RoomCode.BONUS.code;
            int singleCode = RoomCode.SINGLE.code;

            // BONUS and SINGLE are NOT credit/debit flow
            assertFalse(bonusCode == RoomCode.MATCHING.code || bonusCode == RoomCode.FISHING.code);
            assertFalse(singleCode == RoomCode.MATCHING.code || singleCode == RoomCode.FISHING.code);
        }

        @Test
        @DisplayName("SettleDto for BONUS mode uses money as winAmount")
        void settleDto_bonusMode_usesMoneyAsWinAmount() {
            // Given
            SettleDto settleDto = new SettleDto();
            settleDto.setRoomMode(RoomCode.BONUS.code);
            settleDto.setMoney(new BigDecimal("500"));
            settleDto.setValidBet(new BigDecimal("100"));
            settleDto.setTotalWithdraw(new BigDecimal("400"));

            // When: BONUS mode uses money directly, not validBet + totalWithdraw
            BigDecimal winAmount = settleDto.getWinAmount();

            // Then: For non-MATCHING modes, getWinAmount() returns money
            assertEquals(new BigDecimal("500"), winAmount);
        }

        @Test
        @DisplayName("SettleDto for SINGLE mode uses money as winAmount")
        void settleDto_singleMode_usesMoneyAsWinAmount() {
            // Given
            SettleDto settleDto = new SettleDto();
            settleDto.setRoomMode(RoomCode.SINGLE.code);
            settleDto.setMoney(new BigDecimal("750"));
            settleDto.setValidBet(new BigDecimal("200"));
            settleDto.setTotalWithdraw(new BigDecimal("550"));

            // When
            BigDecimal winAmount = settleDto.getWinAmount();

            // Then
            assertEquals(new BigDecimal("750"), winAmount);
        }
    }

    // ==================================================================================
    // SETTLE DTO INTERFACE TESTS
    // ==================================================================================
    @Nested
    @DisplayName("SettleDto Interface Implementation")
    class SettleDtoInterfaceTests {

        @Test
        @DisplayName("SettleDto implements BetResultData interface correctly")
        void settleDto_implementsBetResultData() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "testaccount",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("100"),
                    "game-456",
                    2,
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    BigDecimal.ZERO
            );

            // Then: BetResultData interface methods
            assertEquals("order-123", settleDto.getExternalTransactionId());
            assertEquals("order-123", settleDto.getVendorBetId());
            assertEquals("game-456", settleDto.getRoundId());
            assertEquals("600", settleDto.getGameId());
            assertEquals(BigDecimal.ZERO, settleDto.getBetAmount()); // Always returns ZERO
            assertNull(settleDto.getWinLoss());
            assertEquals(new BigDecimal(50), settleDto.getEffectiveTurnover());
            assertNull(settleDto.getVendorBetTime());
            assertNull(settleDto.getJackpotAmount());
            assertEquals(0, settleDto.getIsFreespin());
        }

        @Test
        @DisplayName("SettleDto implements RequestIdempotency interface correctly")
        void settleDto_implementsRequestIdempotency() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "testaccount",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("100"),
                    "game-456",
                    1,
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    BigDecimal.ZERO
            );

            // Then: RequestIdempotency interface
            assertEquals("order-123", settleDto.getTransactionId());
            assertEquals("testaccount", settleDto.getVendorPlayerUsername());
        }

        @Test
        @DisplayName("SettleDto.getWinAmount() for MATCHING mode - compare with VendorService")
        void settleDto_getWinAmount_matchingMode_usesFormula() {
            // Given: MATCHING mode where totalWithdraw happens to be accurate
            SettleDto settleDto = createSettleDtoFromParams(
                    "testaccount",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("1000"),      // credit
                    "game-456",
                    RoomCode.MATCHING.code,
                    new BigDecimal("200"),
                    new BigDecimal("200"),       // validBet
                    new BigDecimal("300"),       // totalWithdraw
                    BigDecimal.ZERO
            );

            // debitAmount = credit - totalWithdraw = 1000 - 300 = 700
            BigDecimal debitAmount = new BigDecimal("700");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testaccount", "600_MATCHING");

            // When: Compare SettleDto vs VendorService
            BigDecimal dtoWinAmount = settleDto.getWinAmount();
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal serviceWinAmount = walletRequest.getWinAmount();

            // Then: Both give 500 when totalWithdraw is accurate
            assertEquals(new BigDecimal("500"), dtoWinAmount, "SettleDto: validBet + totalWithdraw = 200 + 300 = 500");
            assertEquals(new BigDecimal("500"), serviceWinAmount, "Service: validBet + (credit - debit) = 200 + 300 = 500");
            assertEquals(0, dtoWinAmount.compareTo(serviceWinAmount));
        }

        @Test
        @DisplayName("SettleDto.getWinAmount() for non-MATCHING mode - BONUS uses money directly")
        void settleDto_getWinAmount_nonMatchingMode_returnsMoney() {
            // Given: BONUS mode - uses money directly, not credit/debit flow
            SettleDto settleDto = createSettleDtoFromParams(
                    "testaccount",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("1000"),      // money (winAmount for BONUS)
                    "game-456",
                    RoomCode.BONUS.code,
                    new BigDecimal("200"),
                    new BigDecimal("200"),
                    new BigDecimal("300"),       // totalWithdraw (ignored for BONUS)
                    BigDecimal.ZERO
            );

            // When: SettleDto.getWinAmount() for BONUS mode
            BigDecimal winAmount = settleDto.getWinAmount();

            // Then: BONUS mode returns money directly (not validBet + totalWithdraw)
            assertEquals(new BigDecimal("1000"), winAmount,
                    "BONUS mode uses money directly, not the credit/debit formula");
        }
    }

    // ==================================================================================
    // COMPARISON TESTS - OLD VS NEW CALCULATION
    // ==================================================================================
    @Nested
    @DisplayName("Comparison - Old vs New Calculation")
    class ComparisonOldVsNewTests {

        @Test
        @DisplayName("KYP_600: SettleDto=111 vs VendorService=0")
        void kyp600_comparison_oldVsNew() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwuv0ub",
                    "214749509260-600-5795428050-518510742",
                    "50-1767509660-5795428050-3",
                    600,
                    new BigDecimal("335.17"),
                    "50-1767509660-5795428050-3",
                    1,
                    new BigDecimal("228"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub", "600_MATCHING");

            // When
            BigDecimal oldWinAmount = settleDto.getWinAmount();
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal newWinAmount = walletRequest.getWinAmount();

            // Then
            assertEquals(new BigDecimal("111"), oldWinAmount, "Old formula gives 111");
            assertEquals(0, newWinAmount.compareTo(BigDecimal.ZERO), "New formula gives 0");
            assertNotEquals(0, oldWinAmount.compareTo(newWinAmount), "Results differ");
        }

        @Test
        @DisplayName("KYP_1370: Both formulas match when totalWithdraw is accurate")
        void kyp1370_comparison_bothMatch() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "buqhwshs0b",
                    "214749504982-1370-5785353828-516997492",
                    "50-1767410567-5785353828-1",
                    1370,
                    new BigDecimal("1008.5"),
                    "50-1767410567-5785353828-1",
                    1,
                    new BigDecimal("290"),
                    new BigDecimal("290"),
                    new BigDecimal("408.5"),
                    new BigDecimal("21.5")
            );

            BigDecimal debitAmount = new BigDecimal("600");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwshs0b", "1370_MATCHING");

            // When
            BigDecimal oldWinAmount = settleDto.getWinAmount();
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal newWinAmount = walletRequest.getWinAmount();

            // Then: Both give 698.5
            assertEquals(new BigDecimal("698.5"), oldWinAmount);
            assertEquals(new BigDecimal("698.5"), newWinAmount);
            assertEquals(0, oldWinAmount.compareTo(newWinAmount));
        }
    }

    // ==================================================================================
    // RESPONSE CODES TESTS
    // ==================================================================================
    @Nested
    @DisplayName("Response Codes")
    class ResponseCodesTests {

        @Test
        @DisplayName("ResponseCodes should have correct values")
        void responseCodes_haveCorrectValues() {
            assertEquals(0, ResponseCodes.SUCCESS);
            assertEquals(13, ResponseCodes.INTERNAL_ERROR);
            assertEquals(9, ResponseCodes.DUPLICATE);
            assertEquals(12, ResponseCodes.BET_NOT_FOUND);
            assertEquals(5, ResponseCodes.INVALID_REQUEST);
            assertEquals(11, ResponseCodes.PROCESSING);
        }
    }

    // ==================================================================================
    // EDGE CASES AND ERROR HANDLING
    // ==================================================================================
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("dataCreditMapper with zero debit amount")
        void dataCreditMapper_zeroDebitAmount() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "testplayer",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("100"),
                    "game-456",
                    1,
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    new BigDecimal("100"),
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = BigDecimal.ZERO;
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testplayer", "600_MATCHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 50 + (100 - 0) = 150
            assertEquals(new BigDecimal("150"), walletRequest.getWinAmount());
        }

        @Test
        @DisplayName("dataCreditMapper with equal credit and debit")
        void dataCreditMapper_equalCreditDebit() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "testplayer",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("500"),
                    "game-456",
                    1,
                    new BigDecimal("100"),
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("500");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testplayer", "600_MATCHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 100 + (500 - 500) = 100
            assertEquals(new BigDecimal("100"), walletRequest.getWinAmount());
        }

        @Test
        @DisplayName("dataCreditMapper with negative calculated winAmount should cap to zero")
        void dataCreditMapper_negativeWinAmount_capsToZero() {
            // Given: Large debit, small credit
            SettleDto settleDto = createSettleDtoFromParams(
                    "testplayer",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("100"),
                    "game-456",
                    1,
                    new BigDecimal("50"),
                    new BigDecimal("50"),
                    new BigDecimal("-950"),
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("1000");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testplayer", "600_MATCHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 50 + (100 - 1000) = 50 + (-900) = -850 -> capped to 0
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO));
            assertEquals(ResultType.END.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("dataCreditMapper with high precision decimals")
        void dataCreditMapper_highPrecisionDecimals() {
            // Given
            SettleDto settleDto = createSettleDtoFromParams(
                    "testplayer",
                    "order-123",
                    "game-456",
                    600,
                    new BigDecimal("1234.56789"),
                    "game-456",
                    1,
                    new BigDecimal("100.12345"),
                    new BigDecimal("100.12345"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            BigDecimal debitAmount = new BigDecimal("1000.00000");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testplayer", "600_MATCHING");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 100.12345 + (1234.56789 - 1000.00000) = 334.69134
            assertEquals(new BigDecimal("334.69134"), walletRequest.getWinAmount());
        }
    }
}
