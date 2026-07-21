
package com.embabel.gekko;

import com.embabel.gekko.aot.hint.TraderAgentRuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ImportRuntimeHints;


@SpringBootApplication
@ImportRuntimeHints(TraderAgentRuntimeHintsRegistrar.class)
@ConfigurationPropertiesScan
class GekkoApplication {
    static void main(String[] args) {
        SpringApplication.run(GekkoApplication.class, args);
    }
}
