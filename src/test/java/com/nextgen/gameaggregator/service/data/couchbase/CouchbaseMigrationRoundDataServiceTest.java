package com.nextgen.gameaggregator.service.data.couchbase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nextgen.gameaggregator.entity.couchbase.ApiVersion;
import com.nextgen.gameaggregator.entity.couchbase.RoundMarker;
import com.nextgen.gameaggregator.repository.couchbase.MigrationRoundMarkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * GA-15041: claiming a round and keeping an existing claim alive are separate operations,
 * and a claim that cannot be established must say so rather than fail silently.
 */
public class CouchbaseMigrationRoundDataServiceTest {

    private static final String KEY = "evoplay::2044577169934";
    /** Mirrors the constant in the service; must stay <= the migration_round collection maxTTL. */
    private static final Duration TTL = Duration.ofDays(10);

    private final MigrationRoundMarkerRepository repository = mock(MigrationRoundMarkerRepository.class);
    private final CouchbaseMigrationRoundDataService service =
            new CouchbaseMigrationRoundDataService(repository);

    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger serviceLogger;

    @BeforeEach
    void captureLogs() {
        logs = new ListAppender<>();
        logs.start();
        serviceLogger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(CouchbaseMigrationRoundDataService.class);
        serviceLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        serviceLogger.detachAppender(logs);
    }

    private boolean warned() {
        return logs.list.stream().anyMatch(e -> e.getLevel() == Level.WARN);
    }

    @Test
    void markOnRoundOpenCreatesTheMarkerAndStopsThere() {
        when(repository.insertIfAbsent(eq(KEY), any(), eq(TTL))).thenReturn(true);

        service.markOnRoundOpen("evoplay", "2044577169934");

        ArgumentCaptor<RoundMarker> marker = ArgumentCaptor.forClass(RoundMarker.class);
        verify(repository).insertIfAbsent(eq(KEY), marker.capture(), eq(TTL));
        assertEquals(ApiVersion.V1, marker.getValue().getVersion());
        verify(repository, never()).touchIfPresent(any(), any());
        verify(repository, never()).upsert(any(), any(), any());
    }

    @Test
    void markOnRoundOpenExtendsAMarkerThatAlreadyExists() {
        when(repository.insertIfAbsent(eq(KEY), any(), eq(TTL))).thenReturn(false);
        when(repository.touchIfPresent(KEY, TTL)).thenReturn(true);

        service.markOnRoundOpen("evoplay", "2044577169934");

        verify(repository).insertIfAbsent(eq(KEY), any(), eq(TTL));
        verify(repository).touchIfPresent(KEY, TTL);
    }

    @Test
    void markOnRoundOpenRetriesWhenTheMarkerExpiresBetweenInsertAndTouch() {
        // First pass: marker exists at insert, gone by touch. Second pass wins the insert.
        when(repository.insertIfAbsent(eq(KEY), any(), eq(TTL))).thenReturn(false, true);
        when(repository.touchIfPresent(KEY, TTL)).thenReturn(false);

        service.markOnRoundOpen("evoplay", "2044577169934");

        verify(repository, times(2)).insertIfAbsent(eq(KEY), any(), eq(TTL));
        verify(repository, times(1)).touchIfPresent(KEY, TTL);
        assertTrue(logs.list.isEmpty(), "a recovered race is not worth reporting");
    }

    @Test
    void markOnRoundOpenWarnsWhenBothPassesLoseTheRace() {
        when(repository.insertIfAbsent(eq(KEY), any(), eq(TTL))).thenReturn(false);
        when(repository.touchIfPresent(KEY, TTL)).thenReturn(false);

        service.markOnRoundOpen("evoplay", "2044577169934");

        verify(repository, times(2)).insertIfAbsent(eq(KEY), any(), eq(TTL));
        verify(repository, times(2)).touchIfPresent(KEY, TTL);
        assertTrue(warned(), "an unpinned round must not be left silent");
    }

    @Test
    void touchMarkerNeverCreatesOne() {
        when(repository.touchIfPresent(KEY, TTL)).thenReturn(true);

        service.touchMarker("evoplay", "2044577169934");

        verify(repository).touchIfPresent(KEY, TTL);
        verify(repository, never()).insertIfAbsent(any(), any(), any());
        verify(repository, never()).upsert(any(), any(), any());
    }

    @Test
    void touchMarkerStaysQuietWhenThereIsNoMarker() {
        // The normal case for a v2-owned round: nothing to extend, nothing to report.
        when(repository.touchIfPresent(KEY, TTL)).thenReturn(false);

        service.touchMarker("evoplay", "2044577169934");

        assertTrue(logs.list.isEmpty(), "an absent marker is expected, not an anomaly");
    }

    @Test
    void markOnRoundOpenSwallowsRepositoryFailure() {
        when(repository.insertIfAbsent(any(), any(), any())).thenThrow(new RuntimeException("couchbase down"));

        service.markOnRoundOpen("evoplay", "2044577169934");
    }

    @Test
    void touchMarkerSwallowsRepositoryFailure() {
        when(repository.touchIfPresent(any(), any())).thenThrow(new RuntimeException("couchbase down"));

        service.touchMarker("evoplay", "2044577169934");
    }
}
