package com.embabel.gekko.htmx;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HitlConfig {

    @Value("${app.hitl.ttl-hours:24}")
    private int ttlHours;

    @Bean
    public HitlService hitlService() {
        return new HitlService(Duration.ofHours(ttlHours));
    }
}
