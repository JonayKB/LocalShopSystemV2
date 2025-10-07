package es.jonay.kb.shopsystem.api.tasks;

import jakarta.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.jonay.kb.shopsystem.api.services.MailService;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class StockTasks {
    private final Logger logger = Logger.getLogger(StockTasks.class.getName());
    private final IItemRepository itemRepository;
    private final MailService mailService;

    public StockTasks(IItemRepository itemRepository, MailService mailService) {
        this.itemRepository = itemRepository;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkOutOfStockItems() {
        logger.log(Level.INFO, "EXECUTED TASK CHECK OUT ITEMS");

        List<Item> outOfStockItems = itemRepository.getOutOfStock();

        // ENVIAR NOTIFICACION DICIENDO QUE FALTAN STOCK DE ESTOS ITEMS

        if (!outOfStockItems.isEmpty()) {
            mailService.sendOutOfStockEmail(outOfStockItems);
        }

    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkUnderBareMinimumItems() {
        logger.log(Level.INFO, "EXECUTED TASK CHECK BARE MINIMUM");

        List<Item> underBareMinimunItems = itemRepository.getUnderBareMinimun();

        // ENVIAR NOTIFACION DICIENDO EL STOCK QUE TIENE, Y CUANTO DEBE DE TENER

        if (!underBareMinimunItems.isEmpty()) {
            mailService.sendUnderBareMinimunItemsEmail(underBareMinimunItems);
        }
    }
}
