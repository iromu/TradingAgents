package com.embabel.template;


import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.template.agent.TraderAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.UUID;

@ShellComponent
public class TickerShellCommands {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.embabel.template");
    private final AgentPlatform agentPlatform;

    public TickerShellCommands(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    @ShellMethod("Analyse ticker symbol")
    public String ticker(
            @ShellOption(help = "Enter the ticker symbol to analyse", value = "ticker", defaultValue = "SPY") String ticker) {

        Span span = tracer.spanBuilder("user-interaction")
//                .setAttribute("langfuse.user.id", userId)
                .setAttribute("langfuse.session.id", UUID.randomUUID().toString())
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            var output = AgentInvocation
                    .builder(agentPlatform)
                    .options(ProcessOptions.builder().verbosity(v -> v.showPrompts(true)).build())
                    .build(TraderAgent.InvestmentDebateState.class)
                    .invoke(new UserInput(ticker));
            return format(output);
        } finally {
            span.end();
        }
    }

    // Use JSON pretty printer to format the result
    private String format(Object result) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
