package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.Expiry;
import com.couchbase.client.java.kv.MutateInOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameTransactionRepositoryTest {

    private static final String DOC_ID = "evoplay::BET::vendor-bet-1";

    @Mock
    private Collection collection;

    // ONEAPI-2065: a write without a TTL must keep the doc's existing expiry. Sending no
    // expiry resets it to "never", which is how alias BET docs lost their 3-day TTL.
    @Test
    void update_preservesExpiry_whenTtlNull() {
        new GameTransactionRepository(collection).update(DOC_ID, Map.of("status", "SUCCESS"), null);

        MutateInOptions.Built options = captureOptions();
        assertTrue(options.preserveExpiry());
        assertEquals(Expiry.none().toString(), options.expiry().toString());
    }

    @Test
    void update_setsExpiry_whenTtlGiven() {
        Duration ttl = Duration.ofHours(3);

        new GameTransactionRepository(collection).update(DOC_ID, Map.of("state", "SETTLED"), ttl);

        MutateInOptions.Built options = captureOptions();
        assertFalse(options.preserveExpiry());
        assertEquals(Expiry.relative(ttl).toString(), options.expiry().toString());
    }

    private MutateInOptions.Built captureOptions() {
        ArgumentCaptor<MutateInOptions> options = ArgumentCaptor.forClass(MutateInOptions.class);
        verify(collection).mutateIn(eq(DOC_ID), anyList(), options.capture());
        return options.getValue().build();
    }
}
