package es.jonay.kb.shopsystem.api.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.jonay.kb.shopsystem.service.AuthService;

/** Borra cada noche los refresh tokens caducados para que la tabla no crezca sin límite. */
@Component
public class RefreshTokenCleanupTask {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);
    private final AuthService authController;

    public RefreshTokenCleanupTask(AuthService authController) {
        this.authController = authController;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeExpired() {
        int deleted = authController.purgeExpired();
        logger.info("Refresh tokens caducados borrados: " + deleted);
    }
}
