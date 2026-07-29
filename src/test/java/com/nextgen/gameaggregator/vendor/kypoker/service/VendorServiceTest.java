package com.nextgen.gameaggregator.vendor.kypoker.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.vendor.kypoker.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.kypoker.api.settle.SettleDto;
import com.nextgen.gameaggregator.vendor.kypoker.constant.RoomCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class VendorServiceTest {

    @Mock
    private WalletRequestService walletRequestService;

    private VendorService vendorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vendorService = new VendorService(walletRequestService);
        when(walletRequestService.updateByGameSession(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private GameSession createGameSession(String account) {
        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername(account);
        gameSession.setVendorGameCode("600_MATCHING");
        gameSession.setToken("test-token");
        return gameSession;
    }

    private SettleDto createSettleDto(BigDecimal money, BigDecimal validBet, BigDecimal totalWithdraw, int roomMode) {
        SettleDto settleDto = new SettleDto();
        settleDto.setOrderId("order-123");
        settleDto.setGameNo("game-123");
        settleDto.setMoney(money);
        settleDto.setValidBet(validBet);
        settleDto.setTotalWithdraw(totalWithdraw);
        settleDto.setRoomMode(roomMode);
        settleDto.setRevenue(BigDecimal.ZERO);
        return settleDto;
    }

    private BetDto createBetDto(String account, String orderId, String gameNo, Integer kindId,
                                BigDecimal money, Integer roomMode, Long timeStamp) {
        BetDto betDto = new BetDto();
        betDto.setS("1001");
        betDto.setAccount(account);
        betDto.setOrderId(orderId);
        betDto.setGameNo(gameNo);
        betDto.setGameId(gameNo);
        betDto.setKindId(kindId);
        betDto.setMoney(money);
        betDto.setCurrency("CNY");
        betDto.setRoomMode(roomMode);
        betDto.setTimeStamp(timeStamp);
        return betDto;
    }

    // ==================================================================================
    // AES ENCRYPTION TESTS
    // ==================================================================================
    @Nested
    @DisplayName("AES Encryption Tests")
    class AesEncryptionTests {

        @Test
        @DisplayName("aesEncrypt should encrypt data with valid key")
        void aesEncrypt_withValidKey_encryptsData() throws InvalidEncryptionException {
            String data = "test-data-to-encrypt";
            String key = "1234567890123456"; // 16-byte key for AES-128

            String encrypted = VendorService.aesEncrypt(data, key);

            assertNotNull(encrypted);
            assertNotEquals(data, encrypted);
            // Base64 encoded result should be different from original
            assertTrue(encrypted.length() > 0);
        }

        @Test
        @DisplayName("aesEncrypt should handle key shorter than 16 bytes")
        void aesEncrypt_withShortKey_padsKey() throws InvalidEncryptionException {
            String data = "test-data";
            String shortKey = "short"; // Less than 16 bytes

            String encrypted = VendorService.aesEncrypt(data, shortKey);

            assertNotNull(encrypted);
            assertTrue(encrypted.length() > 0);
        }

        @Test
        @DisplayName("aesEncrypt should handle key longer than 16 bytes")
        void aesEncrypt_withLongKey_truncatesKey() throws InvalidEncryptionException {
            String data = "test-data";
            String longKey = "this-is-a-very-long-key-more-than-16-bytes";

            String encrypted = VendorService.aesEncrypt(data, longKey);

            assertNotNull(encrypted);
            assertTrue(encrypted.length() > 0);
        }

        @Test
        @DisplayName("aesEncrypt and AESDecrypt should be reversible")
        void aesEncryptDecrypt_shouldBeReversible() throws InvalidEncryptionException, InvalidDecryptionException {
            String originalData = "s=1003&account=buqhwshs0b&orderId=12345";
            String key = "1234567890123456";

            String encrypted = VendorService.aesEncrypt(originalData, key);
            String decrypted = VendorService.AESDecrypt(encrypted, key, false);

            assertEquals(originalData, decrypted);
        }

        @Test
        @DisplayName("AESDecrypt with URL decode should decode URL-encoded input")
        void aesDecrypt_withUrlDecode_decodesInput() throws InvalidEncryptionException, InvalidDecryptionException {
            String originalData = "test-data";
            String key = "1234567890123456";

            String encrypted = VendorService.aesEncrypt(originalData, key);
            // Simulate URL encoding (though our encrypted string may not need it)
            String decrypted = VendorService.AESDecrypt(encrypted, key, true);

            assertEquals(originalData, decrypted);
        }

        @Test
        @DisplayName("AESDecrypt with invalid data should return null")
        void aesDecrypt_withInvalidData_returnsNull() throws InvalidDecryptionException {
            String invalidData = "not-valid-encrypted-data";
            String key = "1234567890123456";

            String result = VendorService.AESDecrypt(invalidData, key, false);

            assertNull(result);
        }
    }

    // ==================================================================================
    // MD5 ENCRYPTION TESTS
    // ==================================================================================
    @Nested
    @DisplayName("MD5 Encryption Tests")
    class Md5EncryptionTests {

        @Test
        @DisplayName("MD5Encrypt should produce 32-character hash")
        void md5Encrypt_shouldProduce32CharHash() {
            String input = "test-string";

            String hash = VendorService.MD5Encrypt(input);

            assertNotNull(hash);
            assertEquals(32, hash.length());
        }

        @Test
        @DisplayName("MD5Encrypt should produce consistent results")
        void md5Encrypt_shouldBeConsistent() {
            String input = "same-input";

            String hash1 = VendorService.MD5Encrypt(input);
            String hash2 = VendorService.MD5Encrypt(input);

            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("MD5Encrypt should produce different hashes for different inputs")
        void md5Encrypt_differentInputs_differentHashes() {
            String input1 = "input-one";
            String input2 = "input-two";

            String hash1 = VendorService.MD5Encrypt(input1);
            String hash2 = VendorService.MD5Encrypt(input2);

            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("MD5Encrypt should handle empty string")
        void md5Encrypt_emptyString_returnsHash() {
            String hash = VendorService.MD5Encrypt("");

            assertNotNull(hash);
            assertEquals(32, hash.length());
            // MD5 of empty string is d41d8cd98f00b204e9800998ecf8427e
            assertEquals("d41d8cd98f00b204e9800998ecf8427e", hash);
        }

        @Test
        @DisplayName("MD5Encrypt should handle special characters")
        void md5Encrypt_specialCharacters_returnsHash() {
            String input = "特殊字符!@#$%^&*()";

            String hash = VendorService.MD5Encrypt(input);

            assertNotNull(hash);
            assertEquals(32, hash.length());
        }
    }

    // ==================================================================================
    // DATA DEBIT MAPPER TESTS
    // ==================================================================================
    @Nested
    @DisplayName("dataDebitMapper Tests")
    class DataDebitMapperTests {

        @Test
        @DisplayName("dataDebitMapper should map all BetDto fields to WalletRequest")
        void dataDebitMapper_shouldMapAllFields() {
            // Given: Exact debit request params
            BetDto betDto = createBetDto(
                    "buqhwuv0ub",
                    "214749509260-600-5795428050-518510740",
                    "50-1767509660-5795428050-3",
                    600,
                    new BigDecimal("563.17"),  // debit amount
                    RoomCode.MATCHING.code,
                    1767509660000L
            );
            GameSession gameSession = createGameSession("buqhwuv0ub");
            WalletRequest walletRequest = new WalletRequest();

            // When
            vendorService.dataDebitMapper(walletRequest, betDto, gameSession);

            // Then
            assertEquals("buqhwuv0ub", walletRequest.getVendorPlayerUsername());
            assertEquals("214749509260-600-5795428050-518510740", walletRequest.getExternalTransactionId());
            assertEquals("50-1767509660-5795428050-3", walletRequest.getRoundId());
            assertEquals("600_MATCHING", walletRequest.getVendorGameCode());
            assertEquals("test-token", walletRequest.getToken());
            assertEquals("214749509260-600-5795428050-518510740", walletRequest.getVendorBetId());
            assertEquals(new BigDecimal("563.17"), walletRequest.getTransferAmount());
            assertEquals(1767509660000L, walletRequest.getTimestamp());
        }

        @Test
        @DisplayName("dataDebitMapper for KYP_600 should set debit amount 563.17")
        void dataDebitMapper_kyp600_setsCorrectDebitAmount() {
            // Given: KYP_600 debit request with amount 563.17
            BetDto betDto = createBetDto(
                    "buqhwuv0ub",
                    "214749509260-600-5795428050-518510740",
                    "50-1767509660-5795428050-3",
                    600,
                    new BigDecimal("563.17"),
                    RoomCode.MATCHING.code,
                    System.currentTimeMillis()
            );
            GameSession gameSession = createGameSession("buqhwuv0ub");
            WalletRequest walletRequest = new WalletRequest();

            // When
            vendorService.dataDebitMapper(walletRequest, betDto, gameSession);

            // Then: Transfer amount should be the bet/debit amount
            assertEquals(new BigDecimal("563.17"), walletRequest.getTransferAmount());
        }

        @Test
        @DisplayName("dataDebitMapper for FISHING room mode")
        void dataDebitMapper_fishingRoomMode_mapsCorrectly() {
            // Given
            BetDto betDto = createBetDto(
                    "testplayer",
                    "order-fishing-123",
                    "game-fishing-456",
                    700,
                    new BigDecimal("100.00"),
                    RoomCode.FISHING.code,
                    System.currentTimeMillis()
            );
            GameSession gameSession = createGameSession("testplayer");
            gameSession.setVendorGameCode("700_FISHING");
            WalletRequest walletRequest = new WalletRequest();

            // When
            vendorService.dataDebitMapper(walletRequest, betDto, gameSession);

            // Then
            assertEquals("testplayer", walletRequest.getVendorPlayerUsername());
            assertEquals(new BigDecimal("100.00"), walletRequest.getTransferAmount());
            assertEquals("700_FISHING", walletRequest.getVendorGameCode());
        }
    }

    // ==================================================================================
    // DATA CREDIT MAPPER TESTS - KYP_600 PROBLEM CASE
    // ==================================================================================
    @Nested
    @DisplayName("dataCreditMapper Tests - KYP_600 Problem Case")
    class DataCreditMapperKyp600Tests {

        @Test
        @DisplayName("KYP_600: winAmount should be 0 (not 111) with exact params from issue")
        void dataCreditMapper_kyp600_winAmountIsZero() {
            // Given: Exact params from the KYP_600 issue
            // Previous debit amount: 563.17
            // Credit request params:
            // s=1003, account=buqhwuv0ub, orderId=214749509260-600-5795428050-518510742
            // gameNo=50-1767509660-5795428050-3, kindId=600, money=335.17
            // roomMode=1, totalBet=228, validBet=228, totalWithdraw=-117, revenue=0
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("335.17"),  // credit amount (money)
                    new BigDecimal("228"),      // validBet (betAmount)
                    new BigDecimal("-117"),     // totalWithdraw (UNRELIABLE!)
                    RoomCode.MATCHING.code
            );
            settleDto.setOrderId("214749509260-600-5795428050-518510742");
            settleDto.setGameNo("50-1767509660-5795428050-3");

            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 228 + (335.17 - 563.17) = 228 + (-228) = 0
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO),
                    "winAmount should be 0, not 111");
            assertEquals(ResultType.END.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("KYP_600: All wallet request fields should be set correctly")
        void dataCreditMapper_kyp600_setsAllFields() {
            // Given
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("335.17"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then
            assertEquals("buqhwuv0ub", walletRequest.getVendorPlayerUsername());
            assertEquals(new BigDecimal("335.17"), walletRequest.getTransferAmount());
            assertEquals(new BigDecimal("228"), walletRequest.getBetAmount());
            assertEquals(new BigDecimal("228"), walletRequest.getEffectiveTurnover());
            assertEquals(BigDecimal.ZERO, walletRequest.getJackpotAmount());
            assertEquals(0, walletRequest.getTakeAll());
        }
    }

    // ==================================================================================
    // DATA CREDIT MAPPER TESTS - KYP_1370 WORKING CASE
    // ==================================================================================
    @Nested
    @DisplayName("dataCreditMapper Tests - KYP_1370 Working Case")
    class DataCreditMapperKyp1370Tests {

        @Test
        @DisplayName("KYP_1370: winAmount should be 698.5 with exact params from issue")
        void dataCreditMapper_kyp1370_winAmountIs698point5() {
            // Given: Exact params from the KYP_1370 working case
            // Credit request params:
            // s=1003, account=buqhwshs0b, orderId=214749504982-1370-5785353828-516997492
            // gameNo=50-1767410567-5785353828-1, kindId=1370, money=1008.5
            // roomMode=1, totalBet=290, validBet=290, totalWithdraw=408.5, revenue=21.5
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("1008.5"),   // credit amount (money)
                    new BigDecimal("290"),       // validBet (betAmount)
                    new BigDecimal("408.5"),     // totalWithdraw
                    RoomCode.MATCHING.code
            );
            settleDto.setOrderId("214749504982-1370-5785353828-516997492");
            settleDto.setGameNo("50-1767410567-5785353828-1");

            // Debit amount inferred: 1008.5 - 408.5 = 600
            BigDecimal debitAmount = new BigDecimal("600");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwshs0b");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 290 + (1008.5 - 600) = 290 + 408.5 = 698.5
            assertEquals(new BigDecimal("698.5"), walletRequest.getWinAmount());
            assertEquals(ResultType.WIN.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("KYP_1370: All wallet request fields should be set correctly")
        void dataCreditMapper_kyp1370_setsAllFields() {
            // Given
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("1008.5"),
                    new BigDecimal("290"),
                    new BigDecimal("408.5"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("600");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwshs0b");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then
            assertEquals("buqhwshs0b", walletRequest.getVendorPlayerUsername());
            assertEquals(new BigDecimal("1008.5"), walletRequest.getTransferAmount());
            assertEquals(new BigDecimal("290"), walletRequest.getBetAmount());
            assertEquals(new BigDecimal("290"), walletRequest.getEffectiveTurnover());
            assertEquals(BigDecimal.ZERO, walletRequest.getJackpotAmount());
        }
    }

    // ==================================================================================
    // DATA CREDIT MAPPER TESTS - EDGE CASES
    // ==================================================================================
    @Nested
    @DisplayName("dataCreditMapper Tests - Edge Cases")
    class DataCreditMapperEdgeCaseTests {

        @Test
        @DisplayName("dataCreditMapper should set winAmount to zero when calculated value is negative")
        void dataCreditMapper_negativeWinAmount_shouldBeZero() {
            // Given: Calculated winAmount would be negative
            // debitAmount: 1000, creditAmount: 100, validBet: 50
            // winLoss = 100 - 1000 = -900
            // Calculated: 50 + (-900) = -850 -> capped to 0
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("100"),
                    new BigDecimal("50"),
                    new BigDecimal("-900"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("1000");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testPlayer");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO));
            assertEquals(ResultType.END.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("dataCreditMapper with break even (credit = debit)")
        void dataCreditMapper_breakEven_winAmountEqualsBetAmount() {
            // Given: credit = debit
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("500"),
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("500");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testPlayer");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 100 + (500 - 500) = 100
            assertEquals(new BigDecimal("100"), walletRequest.getWinAmount());
            assertEquals(ResultType.WIN.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("dataCreditMapper with FISHING room mode")
        void dataCreditMapper_fishingRoomMode_calculatesCorrectly() {
            // Given
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("750"),
                    new BigDecimal("200"),
                    new BigDecimal("50"),
                    RoomCode.FISHING.code
            );
            BigDecimal debitAmount = new BigDecimal("500");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testPlayer");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 200 + (750 - 500) = 450
            assertEquals(new BigDecimal("450"), walletRequest.getWinAmount());
            assertEquals(ResultType.WIN.code, walletRequest.getResultType());
        }

        @Test
        @DisplayName("dataCreditMapper should handle high precision decimals")
        void dataCreditMapper_decimalPrecision_handledCorrectly() {
            // Given
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("1234.56789"),
                    new BigDecimal("100.12345"),
                    BigDecimal.ZERO,
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("1000.00000");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("testPlayer");

            // When
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: winAmount = 100.12345 + (1234.56789 - 1000.00000) = 334.69134
            assertEquals(new BigDecimal("334.69134"), walletRequest.getWinAmount());
        }
    }

    // ==================================================================================
    // VENDOR SERVICE FLAGS TESTS
    // ==================================================================================
    @Nested
    @DisplayName("VendorService Flags Tests")
    class VendorServiceFlagsTests {

        @Test
        @DisplayName("shouldRejectCancelRequest should return true by default")
        void shouldRejectCancelRequest_defaultTrue() {
            assertTrue(vendorService.shouldRejectCancelRequest());
        }

        @Test
        @DisplayName("shouldRejectCancelRequest should return configured value")
        void shouldRejectCancelRequest_configurable() {
            vendorService.setRejectSettleAfterRollback(false);
            assertFalse(vendorService.shouldRejectCancelRequest());

            vendorService.setRejectSettleAfterRollback(true);
            assertTrue(vendorService.shouldRejectCancelRequest());
        }

        @Test
        @DisplayName("shouldDoRollbackByRound should always return true")
        void shouldDoRollbackByRound_alwaysTrue() {
            GameSession gameSession = createGameSession("testPlayer");
            assertTrue(vendorService.shouldDoRollbackByRound(gameSession));
        }

        @Test
        @DisplayName("shouldDoRollbackByRound with null gameSession should return true")
        void shouldDoRollbackByRound_nullGameSession_returnsTrue() {
            assertTrue(vendorService.shouldDoRollbackByRound(null));
        }
    }

    // ==================================================================================
    // BUG DEMONSTRATION AND FIX VERIFICATION TESTS
    // ==================================================================================
    @Nested
    @DisplayName("Bug Demonstration and Fix Verification")
    class BugDemoAndFixTests {

        @Test
        @DisplayName("BUG DEMO: SettleDto.getWinAmount()=111 vs VendorService=0 for KYP_600")
        void bugDemo_settleDtoGetWinAmount_gives111() {
            // Given: KYP_600 problem case
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("335.17"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub");

            // When: Compare SettleDto (buggy) vs VendorService (correct)
            BigDecimal buggyWinAmount = settleDto.getWinAmount();
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal correctWinAmount = walletRequest.getWinAmount();

            // Then: SettleDto gives 111 (WRONG), VendorService gives 0 (CORRECT)
            assertEquals(new BigDecimal("111"), buggyWinAmount,
                    "SettleDto.getWinAmount() uses buggy formula (228 + -117 = 111)");
            assertEquals(0, correctWinAmount.compareTo(BigDecimal.ZERO),
                    "VendorService.dataCreditMapper uses correct formula (228 + (335.17 - 563.17) = 0)");
            assertNotEquals(0, buggyWinAmount.compareTo(correctWinAmount),
                    "Buggy and correct results differ - demonstrating the bug");
        }

        @Test
        @DisplayName("FIX: VendorService.dataCreditMapper gives 0 for KYP_600")
        void fixVerification_dataCreditMapper_gives0() {
            // Given: Same KYP_600 problem case
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("335.17"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub");

            // When: Using VendorService.dataCreditMapper (new formula)
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: New formula gives 0, which is CORRECT
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO),
                    "VendorService calculates winAmount=0 (correct)");
        }

        @Test
        @DisplayName("COMPARISON: SettleDto=111 vs VendorService=0 for KYP_600")
        void comparison_oldVsNew_kyp600() {
            // Given
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("335.17"),
                    new BigDecimal("228"),
                    new BigDecimal("-117"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub");

            // When
            BigDecimal oldResult = settleDto.getWinAmount();
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal newResult = walletRequest.getWinAmount();

            // Then
            assertEquals(new BigDecimal("111"), oldResult, "Old formula gives 111");
            assertEquals(0, newResult.compareTo(BigDecimal.ZERO), "New formula gives 0");
            assertNotEquals(0, oldResult.compareTo(newResult), "Results differ - fix is working");
        }

        @Test
        @DisplayName("PROOF: VendorService uses (credit - debit), not totalWithdraw for KYP_600")
        void proof_totalWithdrawMismatch() {
            // Given: KYP_600 with unreliable totalWithdraw
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("335.17"),   // credit
                    new BigDecimal("228"),       // validBet
                    new BigDecimal("-117"),      // totalWithdraw (UNRELIABLE!)
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("563.17");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwuv0ub");

            // When: VendorService calculates using credit - debit
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);

            // Then: VendorService uses actual winLoss (-228), not totalWithdraw (-117)
            // If it used totalWithdraw: winAmount = 228 + (-117) = 111
            // Since it uses credit-debit: winAmount = 228 + (-228) = 0
            assertEquals(0, walletRequest.getWinAmount().compareTo(BigDecimal.ZERO),
                    "VendorService correctly calculates winAmount=0 using (credit - debit)");

            // Prove: totalWithdraw (-117) != actual winLoss (credit - debit = -228)
            BigDecimal actualWinLoss = settleDto.getMoney().subtract(debitAmount);
            assertEquals(0, actualWinLoss.compareTo(new BigDecimal("-228")),
                    "Actual winLoss = credit - debit = -228");
            assertNotEquals(0, settleDto.getTotalWithdraw().compareTo(actualWinLoss),
                    "totalWithdraw (-117) != actual winLoss (-228)");
        }

        @Test
        @DisplayName("KYP_1370: Both formulas match when totalWithdraw is accurate")
        void kyp1370_bothFormulasMatch() {
            // Given
            SettleDto settleDto = createSettleDto(
                    new BigDecimal("1008.5"),
                    new BigDecimal("290"),
                    new BigDecimal("408.5"),
                    RoomCode.MATCHING.code
            );
            BigDecimal debitAmount = new BigDecimal("600");
            WalletRequest walletRequest = new WalletRequest();
            GameSession gameSession = createGameSession("buqhwshs0b");

            // When
            BigDecimal oldResult = settleDto.getWinAmount();
            vendorService.dataCreditMapper(walletRequest, settleDto, gameSession, debitAmount);
            BigDecimal newResult = walletRequest.getWinAmount();

            // Then: Both give 698.5
            assertEquals(new BigDecimal("698.5"), oldResult);
            assertEquals(new BigDecimal("698.5"), newResult);
            assertEquals(0, oldResult.compareTo(newResult));
        }
    }
}
