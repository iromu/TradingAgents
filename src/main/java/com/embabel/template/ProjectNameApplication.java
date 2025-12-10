
package com.embabel.template;

import com.embabel.agent.config.annotation.EnableAgents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableAgents(loggingTheme = "gekko")
class ProjectNameApplication {
    static void main(String[] args) {
        SpringApplication.run(ProjectNameApplication.class, args);
    }
}
