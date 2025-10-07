package es.jonay.kb.shopsystem.api.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResetCacheTask {

        private final Logger logger = Logger.getLogger(ResetCacheTask.class.getName());

    @Autowired
    CacheManager cacheManager;

        @Scheduled(cron = "0 0 3 * * ?")
        public void cleanCache(){
            cacheManager.getCacheNames().stream()
          .forEach(cacheName -> cacheManager.getCache(cacheName).clear());
            logger.log(Level.INFO,"Reset Cache");
        }



    
}
