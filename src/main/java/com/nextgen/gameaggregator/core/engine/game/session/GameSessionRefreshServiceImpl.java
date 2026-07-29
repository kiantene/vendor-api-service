package com.nextgen.gameaggregator.core.engine.game.session;

import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSessionRefreshServiceImpl implements GameSessionRefreshService {

    private final GameSessionRefreshProcessor refreshProcessor;
    private final WalletExceptionTranslator walletExceptionTranslator;

    @Override
    public GameSession execute(GameSessionRefreshContext context) {
        GameSession gameSession = null;

        try {
            // TODO: Idempotency check
            validateContext(context);
            gameSession = refreshProcessor.process(context);
    
        } catch (Exception ex) {
            throw walletExceptionTranslator.translate(ex, context);
        }

        return gameSession;
    }

    private void validateContext(GameSessionRefreshContext context) throws InvalidRequestException {
        if (context == null) {
            throw new InvalidRequestException("Request context is missing");
        }

        if (context.getVendorPlayerUsername() == null) {
            throw new InvalidRequestException("Vendor player username is missing");
        }
    }
}
