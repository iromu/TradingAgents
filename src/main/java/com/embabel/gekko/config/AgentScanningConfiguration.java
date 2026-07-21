package com.embabel.gekko.config;

import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.deployment.AgentScanningProperties;
import com.embabel.agent.spi.support.AgentScanningPostProcessorDelegate;
import com.embabel.agent.spi.support.DelegatingAgentScanningBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ApplicationObjectSupport;

/**
 * Registers the Embabel agent scanning BeanPostProcessor so that @Agent-annotated
 * classes are automatically registered as com.embabel.agent.core.Agent beans.
 *
 * <p>In Embabel 0.5.0-SNAPSHOT the scanning infrastructure exists but is not wired
 * into the auto-configuration, so we register it manually here.
 *
 * TODO: Revisit in next Embabel upgrade — the embabel-agent-starter-webmvc may auto-wire
 * agent scanning, making this manual SPI configuration redundant.
 */
@Configuration
@ConditionalOnProperty(prefix = "embabel.agent.platform", name = "scanning.annotation", havingValue = "true", matchIfMissing = true)
public class AgentScanningConfiguration {

    @Bean
    public static DelegatingAgentScanningBeanPostProcessor delegatingAgentScanningBeanPostProcessor(
            org.springframework.context.ApplicationContext applicationContext,
            ApplicationEventPublisher publisher
    ) {
        return new DelegatingAgentScanningBeanPostProcessor(applicationContext, publisher);
    }

    @Bean
    public static AgentScanningPostProcessorDelegate agentScanningPostProcessorDelegate(
            AgentMetadataReader metadataReader,
            AgentPlatform agentPlatform,
            AgentScanningProperties scanningProperties
    ) {
        return new AgentScanningPostProcessorDelegate(metadataReader, agentPlatform, scanningProperties);
    }
}
