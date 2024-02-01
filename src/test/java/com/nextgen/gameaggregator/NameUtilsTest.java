package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.util.NameUtils;
import org.junit.jupiter.api.Test;

public class NameUtilsTest {

    @Test
    void testUsername1LineAnd1Players() {
        Long agentPlayerId = 1L;
        Long vendorLineId = 1L;
        Integer currencyId = 1;

        String username = NameUtils.generateUsername(vendorLineId, agentPlayerId) + NameUtils.excelColumnNameFormula(currencyId);
        System.out.println("===== 1 Vendor Line + 1 Player + 1 Currency  =====");
        System.out.println(username+ " (" + username.length() + ")");
    }

    @Test
    void testUsername999LinesAnd9MilPlayers() {
        Long agentPlayerId = 999999999L; // 9,999,999
        Long vendorLineId = 999L;
        Integer currencyId = 999;

        String username = NameUtils.generateUsername(vendorLineId, agentPlayerId) + NameUtils.excelColumnNameFormula(currencyId);
        System.out.println("===== 999 Vendor Line + 9 Mil Players  + 999 Currency =====");
        System.out.println(username + " (" + username.length()  + ")");
    }

    @Test
    void testUsername999LinesAnd999MilPlayers() {
        Long agentPlayerId = 99999999999L; // 999,999,999
        Long vendorLineId = 999L;
        Integer currencyId = 999;

        String username = NameUtils.generateUsername(vendorLineId, agentPlayerId) + NameUtils.excelColumnNameFormula(currencyId);
        System.out.println("===== 999 Vendor Line + 999 Currency  + 999 Mil Players + 999 Currency =====");
        System.out.println(username + " (" + username.length() + ")");
    }

    @Test
    void testUsername999LinesAnd99BilPlayers() {
        Long agentPlayerId = 9999999999999L; // 99,999,999,999
        Long vendorLineId = 999L;
        Integer currencyId = 999;

        String username = NameUtils.generateUsername(vendorLineId, agentPlayerId) + NameUtils.excelColumnNameFormula(currencyId);
        System.out.println("===== 999 Vendor Line + 99 Bil Players + 999 Currency  =====");
        System.out.println(username  + " (" + username.length()  +  ")");
    }
}
