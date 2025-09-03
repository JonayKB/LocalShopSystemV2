package es.jonay.kb.shopsystem.api.tasks;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import es.jonay.kb.shopsystem.api.services.MailService;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class StockTasks {
    private final Logger logger = Logger.getLogger(StockTasks.class.getName());
    private static final  Integer WAIT_TO_EXECUTE_SECONDS = 10;//600;
    private final IItemRepository itemRepository;
    private final MailService mailService;

    public StockTasks(IItemRepository itemRepository, MailService mailService) {
        this.itemRepository = itemRepository;
        this.mailService = mailService;
    }

    @PostConstruct
    public void runOnStartupWithDelay() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(this::checkOutOfStockItems, WAIT_TO_EXECUTE_SECONDS, TimeUnit.SECONDS);

        scheduler.schedule(this::checkUnderBareMinimumItems, WAIT_TO_EXECUTE_SECONDS, TimeUnit.SECONDS);

        scheduler.schedule(scheduler::shutdown, WAIT_TO_EXECUTE_SECONDS + 5L, TimeUnit.SECONDS);
    }

    public void checkOutOfStockItems() {
        logger.log(Level.INFO, "EXECUTED TASK CHECK OUT ITEMS");

        List<Item> outOfStockItems = itemRepository.getOutOfStock();

        // ENVIAR NOTIFICACION DICIENDO QUE FALTAN STOCK DE ESTOS ITEMS

        if (!outOfStockItems.isEmpty()) {
            mailService.sendOutOfStockEmail(outOfStockItems);
        }

    }

    public void checkUnderBareMinimumItems() {
        logger.log(Level.INFO, "EXECUTED TASK CHECK BARE MINIMUM");

        List<Item> underBareMinimunItems = itemRepository.getUnderBareMinimun();

        // ENVIAR NOTIFACION DICIENDO EL STOCK QUE TIENE, Y CUANTO DEBE DE TENER

        if (!underBareMinimunItems.isEmpty()) {
            mailService.sendUnderBareMinimunItemsEmail(underBareMinimunItems);
        }
    }
}
