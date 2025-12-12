
package com.embabel.gekko;

import com.embabel.agent.config.annotation.EnableAgents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableAgents(loggingTheme = "gekko")
class GekkoApplication {
    static void main(String[] args) {
        SpringApplication.run(GekkoApplication.class, args);
    }
}
