package com.embabel.gekko.htmx;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HitlConfig {

    @Bean
    public HitlService hitlService() {
        return new HitlService(Duration.ofHours(24));
    }
}
