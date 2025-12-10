package com.embabel.template;


import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public record TickerShellCommands(
        AgentPlatform agentPlatform
) {

    @ShellMethod("Analyse ticker symbol")
    public String ticker(
            @ShellOption(help = "Enter the ticker symbol to analyse", value = "ticker", defaultValue = "SPY") String ticker) {
        var output = AgentInvocation
                .builder(agentPlatform)
                .options(ProcessOptions.builder().verbosity(v -> v.showPrompts(true)).build())
                .build(String.class)
                .invoke(new UserInput(ticker));
        return format(output);
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
