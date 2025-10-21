package com.yanapure.app.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger {
    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    @EventListener(ApplicationStartedEvent.class)
    public void onStart() {
        log.info("Yana Backend started. Logs at logs/app.log (secrets masked).");
    }
}
