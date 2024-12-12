package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.repository.ga.reader.VendorReaderRepository;
import com.nextgen.gameaggregator.repository.ga.writer.CurrencyRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorCurrencyRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorLanguageCodeRepository;
import com.nextgen.gameaggregator.service.CurrencyService;
import com.nextgen.gameaggregator.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class VendorServiceTest {

    @Mock
    private VendorCurrencyRepository vendorCurrencyRepository;
    @Mock
    private VendorReaderRepository vendorReaderRepository;
    @Mock
    private VendorLanguageCodeRepository vendorLanguageCodeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    private CurrencyService currencyService;
    private VendorService vendorService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
        this.currencyService = new CurrencyService(currencyRepository);
        this.vendorService = new VendorService(vendorReaderRepository, vendorLanguageCodeRepository, vendorCurrencyRepository, currencyService);

        //vendorCurrencyRepository.findByVendorIdAndCurrencyId(vendorId, currencyId);

        Integer vendorId = 1;
        Integer currencyId = 1;

        VendorCurrency vendorCurrency = new VendorCurrency();
        vendorCurrency.setVendorId(vendorId);
        vendorCurrency.setCurrencyId(currencyId);
        vendorCurrency.setToVendorRate(BigDecimal.TEN);
        vendorCurrency.setFromVendorRate(BigDecimal.TEN);
        vendorCurrency.setStatus(1);
        when(vendorCurrencyRepository.findByVendorIdAndCurrencyId(vendorId, currencyId)).thenReturn((vendorCurrency));
    }

//    @Test
//    void testGetCurrencyConversionRate() throws VendorCurrencyNotSupportException {
//        WalletRequest walletRequest = new WalletRequest();
//        Integer vendorId = 1;
//        Integer currencyId = 1;
//
//        walletRequest.setVendorId(vendorId);
//        walletRequest.setCurrencyId(currencyId);
//
//        walletRequest = vendorService.getCurrencyConversionRate(walletRequest);
//
//        assertEquals(BigDecimal.TEN, walletRequest.getFromVendorRate());
//        assertEquals(BigDecimal.TEN, walletRequest.getToVendorRate());
//    }

//    @Test
//    void testGetCurrencyConversionRateWithExceptionIsThrown() {
//        Exception exception = assertThrows(VendorCurrencyNotSupportException.class, () -> {
//            WalletRequest walletRequest = new WalletRequest();
//            Integer vendorId = 2;
//            Integer currencyId = 1;
//
//            walletRequest.setVendorId(vendorId);
//            walletRequest.setCurrencyId(currencyId);
//            // Call the method that should throw the exception
//            walletRequest = vendorService.getCurrencyConversionRate(walletRequest);
//        });
//        assertEquals("com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException", exception.toString());
//    }
}
