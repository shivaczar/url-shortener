package org.bitly.scheduler;

import org.bitly.service.RecentShortenedUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaterializedViewScheduler {

    @Autowired
    private RecentShortenedUrlService service;

    @Scheduled(fixedRate = 300000)  // Refresh every 5 minutes
    public void scheduleMaterializedViewRefresh() {
        System.out.println("####################################  Scheduler runs once every 5 minutes ###########################");
        service.refreshMaterializedView();
    }
}

