package es.jonay.kb.shopsystem.api.tasks;

import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.jonay.kb.shopsystem.controller.AuthController;

/** Borra cada noche los refresh tokens caducados para que la tabla no crezca sin límite. */
@Component
public class RefreshTokenCleanupTask {
    private final Logger logger = Logger.getLogger(RefreshTokenCleanupTask.class.getName());
    private final AuthController authController;

    public RefreshTokenCleanupTask(AuthController authController) {
        this.authController = authController;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeExpired() {
        int deleted = authController.purgeExpired();
        logger.info("Refresh tokens caducados borrados: " + deleted);
    }
}
