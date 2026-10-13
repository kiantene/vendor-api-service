package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.api.kv.CoreSubdocMutateCommand;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.DefaultJsonSerializer;
import com.couchbase.client.java.codec.JsonSerializer;
import com.couchbase.client.java.kv.Expiry;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInResult;
import com.couchbase.client.java.kv.MutateInSpec;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.data.model.TxnDelta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRoundRepositoryTest {

    private static final String DOC_ID = "koolbet::player1::round-1";
    private static final JsonSerializer SERIALIZER = DefaultJsonSerializer.create();

    @Mock
    private Collection collection;

    // OVI-2519: updateTxnStateAndBalance must write the slot state AND lastBalance in
    // ONE mutateIn — a single round-doc mutation, not a separate post-success write.
    @Test
    void updateTxnStateAndBalance_writesStateAndBalance_inSingleMutation() {
        GameRoundRepository repo = new GameRoundRepository(collection);

        repo.updateTxnStateAndBalance(DOC_ID, 2, GameRoundState.COMPLETED, new BigDecimal("50.00"));

        Map<String, String> subdoc = captureSubdocWrites();
        assertEquals(2, subdoc.size());
        assertEquals("COMPLETED", subdoc.get("transactions[2].state"));
        assertEquals("50.00", subdoc.get("lastBalance"));
    }

    // A null balance must not add a lastBalance spec, so it can never clobber the last
    // known round balance while still finalizing the rollback slot state.
    @Test
    void updateTxnStateAndBalance_skipsBalanceSpec_whenBalanceNull() {
        GameRoundRepository repo = new GameRoundRepository(collection);

        repo.updateTxnStateAndBalance(DOC_ID, 2, GameRoundState.COMPLETED, null);

        Map<String, String> subdoc = captureSubdocWrites();
        assertEquals(1, subdoc.size());
        assertEquals("COMPLETED", subdoc.get("transactions[2].state"));
        assertFalse(subdoc.containsKey("lastBalance"));
    }

    // ONEAPI-2091: writes that set no TTL must keep the round's existing expiry. Sending
    // no expiry resets it to "never", which is how late writes to ended rounds (e.g. a
    // failed vendor retry appended after the round ended) wiped their 6h TTL.
    @Test
    void appendTxn_preservesExpiry() {
        MutateInResult result = mock(MutateInResult.class);
        when(result.contentAs(1, Long.class)).thenReturn(3L);
        when(collection.mutateIn(eq(DOC_ID), anyList(), any(MutateInOptions.class))).thenReturn(result);

        int idx = new GameRoundRepository(collection).appendTxn(DOC_ID, new RoundTxn());

        assertEquals(2, idx);
        assertPreservesExpiry(captureOptions());
    }

    @Test
    void updateTxn_preservesExpiry() {
        new GameRoundRepository(collection).updateTxn(DOC_ID, 1, Map.of("state", "ERROR"));

        assertPreservesExpiry(captureOptions());
    }

    @Test
    void updateTxnStateAndBalance_preservesExpiry() {
        new GameRoundRepository(collection)
                .updateTxnStateAndBalance(DOC_ID, 2, GameRoundState.COMPLETED, new BigDecimal("50.00"));

        assertPreservesExpiry(captureOptions());
    }

    @Test
    void applyTxnDelta_preservesExpiry_whenDeltaDoesNotEndRound() {
        stubGet();

        new GameRoundRepository(collection).applyTxnDelta(delta(false), Duration.ofHours(6));

        assertPreservesExpiry(captureOptions());
    }

    @Test
    void applyTxnDelta_setsTtl_whenDeltaEndsRound() {
        stubGet();
        Duration ttl = Duration.ofHours(6);

        new GameRoundRepository(collection).applyTxnDelta(delta(true), ttl);

        MutateInOptions.Built options = captureOptions();
        assertFalse(options.preserveExpiry());
        assertEquals(Expiry.relative(ttl).toString(), options.expiry().toString());
    }

    private static TxnDelta delta(boolean isEnded) {
        return TxnDelta.finalizeSuccess(DOC_ID, 0, TxnType.RESULT, "ga-bet-1", new BigDecimal("100.00"),
                null, new BigDecimal("10.00"), null, null, null, null, "10:00:00.000", isEnded, isEnded);
    }

    private void stubGet() {
        when(collection.get(DOC_ID)).thenReturn(mock(GetResult.class));
    }

    private static void assertPreservesExpiry(MutateInOptions.Built options) {
        assertTrue(options.preserveExpiry());
        assertEquals(Expiry.none().toString(), options.expiry().toString());
    }

    private MutateInOptions.Built captureOptions() {
        ArgumentCaptor<MutateInOptions> options = ArgumentCaptor.forClass(MutateInOptions.class);
        verify(collection).mutateIn(eq(DOC_ID), anyList(), options.capture());
        return options.getValue().build();
    }

    // Capture the single mutateIn(DOC_ID, specs) call and decode each spec into a
    // path -> value map using the SDK's own subdoc encoding (no reflection).
    @SuppressWarnings("unchecked")
    private Map<String, String> captureSubdocWrites() {
        ArgumentCaptor<List<MutateInSpec>> specs = ArgumentCaptor.forClass(List.class);
        verify(collection).mutateIn(eq(DOC_ID), specs.capture(), any(MutateInOptions.class));

        return specs.getValue().stream()
                .map(spec -> spec.toCore(SERIALIZER))
                .collect(Collectors.toMap(
                        CoreSubdocMutateCommand::path,
                        cmd -> SERIALIZER.deserialize(String.class, cmd.fragment())));
    }
}
