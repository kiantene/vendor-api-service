package com.nextgen.gameaggregator.core.idempotency;

import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.repository.ga.writer.RequestIdempotentLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestIdempotencyServiceTest {

    @Mock
    private RequestIdempotentLogRepository repository;

    @InjectMocks
    private RequestIdempotencyService service;

    @AfterEach
    void tearDown() {
        RequestIdempotencyService.cleanupThreadLocal();
    }

    /**
     * A duplicate is answered from this record, and a null currency on it is how the vendor is told the
     * original request has not completed yet — see PGSoftExceptionMapper.onDuplicateRequest, which
     * answers PLAYER_OPERATION_IN_PROGRESS rather than reporting success with a balance of zero.
     *
     * <p>Storing a currency here would make an unfinished payout indistinguishable from a finished one
     * and silently disable that guard, so the record must stay currency-less until enrichIdempotentLog
     * writes one on success.
     */
    @Test
    void doesNotStoreCurrencyUntilThePayoutSucceeds() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        service.isDuplicateRequest("PGSoft", "payout", "2087259923267453955-2087259923267453955-403-0");

        verify(repository).save(argThat(log -> log.getCurrency() == null));
    }

    @Test
    void storesTheCurrencyAndBalanceOnceThePayoutSucceeds() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        service.isDuplicateRequest("PGSoft", "payout", "2087259923267453955-2087259923267453955-403-0");

        service.enrichIdempotentLog("01a0556f53bb7e0c", "BRL", new BigDecimal("128.44"));

        // the record is saved twice — empty at creation, then again once the payout lands. Both calls
        // carry the same mutated instance, so assert on the state it was left in by the second.
        ArgumentCaptor<RequestIdempotentLog> saved = ArgumentCaptor.forClass(RequestIdempotentLog.class);
        verify(repository, times(2)).save(saved.capture());

        assertThat(saved.getValue().getCurrency()).isEqualTo("BRL");
        assertThat(saved.getValue().getBalance()).isEqualByComparingTo("128.44");
        assertThat(saved.getValue().getTransactionId()).isEqualTo("01a0556f53bb7e0c");
    }

    @Test
    void reportsAnExistingRecordAsADuplicateAndDoesNotOverwriteIt() {
        RequestIdempotentLog existing = new RequestIdempotentLog();
        existing.setCurrency("BRL");
        existing.setBalance(new BigDecimal("128.44"));
        when(repository.findById(anyString())).thenReturn(Optional.of(existing));

        Optional<RequestIdempotentLog> found =
                service.isDuplicateRequest("PGSoft", "payout", "2087259923267453955-2087259923267453955-403-0");

        assertThat(found).containsSame(existing);
        verify(repository, never()).save(any());
    }
}
