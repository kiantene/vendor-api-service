package com.nextgen.gameaggregator.core.engine.game.session.terminate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminateGameSessionServiceWrapper implements TerminateGameSessionService {

    private final TerminateGameSessionProcessor terminateGameSessionProcessor;

    @Override
    public void process(TerminateGameSessionContext context) {
        terminateGameSessionProcessor.process(context);
    }

}