package com.nextgen.gameaggregator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.nextgen.gameaggregator.service.data.model.TxnAmount;

public class TxnAmountTest {

    @Test
    void testBasicMultiplication() {
        BigDecimal balance = new BigDecimal("10000000000000000000");
        BigDecimal toVendorRate = new BigDecimal("0.001");

        // When
        TxnAmount playerBalance = TxnAmount.of(balance, toVendorRate);

        // Then
        assertEquals(new BigDecimal("10000000000000000"), playerBalance.amount());
    }
}