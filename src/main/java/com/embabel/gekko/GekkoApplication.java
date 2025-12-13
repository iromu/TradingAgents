
package com.embabel.gekko;

import com.embabel.agent.config.annotation.EnableAgents;
import com.embabel.gekko.aot.hint.TraderAgentRuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;


@SpringBootApplication
@EnableAgents(loggingTheme = "gekko")
@ImportRuntimeHints(TraderAgentRuntimeHintsRegistrar.class)
class GekkoApplication {
    static void main(String[] args) {
        SpringApplication.run(GekkoApplication.class, args);
    }
}
