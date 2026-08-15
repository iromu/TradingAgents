package com.embabel.gekko.htmx;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HitlConfig {

    @Value("${app.hitl.ttl-hours:24}")
    private int ttlHours;

    @Bean
    public ScheduledExecutorService hitlCleanupScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public HitlService hitlService(ScheduledExecutorService hitlCleanupScheduler) {
        return new HitlService(Duration.ofHours(ttlHours), hitlCleanupScheduler);
    }
}
