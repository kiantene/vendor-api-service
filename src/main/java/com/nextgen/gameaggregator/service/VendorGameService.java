package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorGameService {

    private final VendorGameRepository vendorGameRepository;

    @Autowired
    public VendorGameService(VendorGameRepository vendorGameRepository) {
        this.vendorGameRepository = vendorGameRepository;
    }

    @Cacheable(value = "VendorGames", key = "#gameId", cacheManager = "cacheManager")
    public VendorGame verifyGameStatus(Integer gameId) throws DisabledGameException {
        VendorGame vendorGame = vendorGameRepository.findById(gameId).orElse(null);
        Optional.ofNullable(vendorGame).orElseThrow(DisabledGameException::new);
        if (!vendorGame.getStatus().equals(Status.ACTIVE.code)) {
            throw new DisabledGameException();
        }
        return vendorGame;
    }

    @Cacheable(value = "VendorGame", key = "{#vendorGameCode, #vendorId}", cacheManager = "cacheManager")
    public VendorGame getByVendorGameCode(String vendorGameCode, Integer vendorId) {
        return vendorGameRepository.findByVendorGameCodeAndVendorId(vendorGameCode, vendorId);
    }

    // deprecated, use getByVendorGameCode so that it will cache null values
    @Cacheable(value = "VendorGames", key = "{#vendorGameCode, #vendorId}", cacheManager = "cacheManager")
    public VendorGame getByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) throws GameNotSupportedException {

        VendorGame vendorGame = vendorGameRepository.findByVendorGameCodeAndVendorId(vendorGameCode, vendorId);
        vendorGame = Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
        if (vendorGame.getStatus() == 0) {
            throw new GameNotSupportedException();
        }
        return vendorGame;
    }

    // ------------------------------------------------------------------------------------------------
    // Why so many by-id lookups?
    // These accreted one call site at a time. We currently have FIVE near-duplicate "get a VendorGame by
    // id" methods (verifyGameStatus, getByVendorGameId, getByGameId, getByVendorGameIdIgnoreStatus,
    // findVendorGameById) that differ only in three orthogonal axes: (a) cache key, (b) not-found
    // behaviour (throw vs null), and (c) whether a status/disabled gate is applied. Nobody consolidated
    // them, so each new need spawned another variant.
    //
    // The sharp edge: getByVendorGameId (status-gated) and getByVendorGameIdIgnoreStatus (no gate) share
    // the SAME cache key ("VendorGames" / {#vendorGameId}). Because @Cacheable returns the cached value
    // without running the method body, a disabled game cached by the no-gate variant is returned to the
    // gated variant on a cache hit WITHOUT its status==0 check. findVendorGameById was added (GA-14768)
    // with a distinct key to avoid feeding that shared slot.
    //
    // TODO(tech-debt): consolidate to ONE raw cached lookup — return Optional<VendorGame> (or null), a
    // single well-namespaced key, no status logic — and move the status/disabled gates into thin,
    // non-cached wrappers (or the callers). Then migrate all call sites, delete the redundant variants,
    // and remove the shared-key hazard. Track under its own cleanup ticket; changing the cache keys/return
    // types touches many callers, so it must not ride a hotfix.
    // ------------------------------------------------------------------------------------------------

    // Status-gated by-id lookup (throws GameNotSupportedException if status==0).
    // NOTE: shares the "VendorGames"/{#vendorGameId} cache key with getByVendorGameIdIgnoreStatus below —
    // on a cache hit populated by that no-gate variant, the status check here is skipped. See TODO above.
    @Cacheable(value = "VendorGames", key = "{#vendorGameId}", cacheManager = "cacheManager")
    public VendorGame getByVendorGameId(Integer vendorGameId) throws GameNotSupportedException {
        VendorGame vendorGame = vendorGameRepository.findById(vendorGameId).orElseThrow(GameNotSupportedException::new);
        if (vendorGame.getStatus() == 0) {
            throw new GameNotSupportedException();
        }
        return vendorGame;
    }

    @Cacheable(value = "VendorGames", key = "{#gameCode}", cacheManager = "cacheManager")
    public VendorGame checkGameSupported(String gameCode) throws GameNotSupportedException, DisabledGameException {
        VendorGame vendorGame = vendorGameRepository.findByCode(gameCode);
        Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
        if (vendorGame.getStatus() == 0) {
            throw new DisabledGameException();
        }

        return vendorGame;
    }

    @Cacheable(value = "VendorGames", key = "#root.methodName + '_' + #gameId", cacheManager = "cacheManager")
    public VendorGame getByGameId(Integer gameId, VendorGame vendorGame) throws GameNotSupportedException {
        if (vendorGame == null) {
            vendorGame = vendorGameRepository.findById(gameId).orElse(null);
            Optional.ofNullable(vendorGame).orElseThrow(GameNotSupportedException::new);
        }

        return vendorGame;
    }

    // Raw by-id lookup, NO status gate. NOTE: shares the SAME "VendorGames"/{#vendorGameId} cache key as
    // getByVendorGameId above — caching a disabled game here can leak to that gated caller on a cache hit.
    // Prefer findVendorGameById for new code (distinct key, returns null). See TODO above.
    @Cacheable(value = "VendorGames", key = "{#vendorGameId}", cacheManager = "cacheManager")
    public VendorGame getByVendorGameIdIgnoreStatus(Integer vendorGameId) throws GameNotSupportedException {
        return vendorGameRepository.findById(vendorGameId).orElseThrow(GameNotSupportedException::new);

    }

    /**
     * GA-14768: cached by-id lookup that returns the raw game (or null) without any status gate.
     * Preferred variant for new code — the yet-another duplicate that exists specifically to avoid the
     * shared-cache-key hazard on the two above (see TODO above). Uses a distinct cache key ('byId_' + id)
     * so it does NOT share the "{#vendorGameId}" slot used by {@link #getByVendorGameId} /
     * {@link #getByVendorGameIdIgnoreStatus} — otherwise caching a disabled game here could be served to
     * getByVendorGameId callers on a cache hit, bypassing their status check. Nulls are not cached,
     * matching the prior always-query-on-miss behaviour of the raw repository call.
     */
    @Cacheable(value = "VendorGames", key = "'byId_' + #vendorGameId", cacheManager = "cacheManager", unless = "#result == null")
    public VendorGame findVendorGameById(Integer vendorGameId) {
        return vendorGameRepository.findById(vendorGameId).orElse(null);
    }
}
