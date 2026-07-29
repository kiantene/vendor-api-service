package com.nextgen.gameaggregator.repository.couchbase;

import com.couchbase.client.core.api.kv.CoreSubdocMutateCommand;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.DefaultJsonSerializer;
import com.couchbase.client.java.codec.JsonSerializer;
import com.couchbase.client.java.kv.MutateInSpec;
import com.nextgen.gameaggregator.enums.GameRoundState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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

    // Capture the single mutateIn(DOC_ID, specs) call and decode each spec into a
    // path -> value map using the SDK's own subdoc encoding (no reflection).
    @SuppressWarnings("unchecked")
    private Map<String, String> captureSubdocWrites() {
        ArgumentCaptor<List<MutateInSpec>> specs = ArgumentCaptor.forClass(List.class);
        verify(collection).mutateIn(eq(DOC_ID), specs.capture());

        return specs.getValue().stream()
                .map(spec -> spec.toCore(SERIALIZER))
                .collect(Collectors.toMap(
                        CoreSubdocMutateCommand::path,
                        cmd -> SERIALIZER.deserialize(String.class, cmd.fragment())));
    }
}
