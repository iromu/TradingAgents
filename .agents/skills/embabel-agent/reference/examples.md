# Example Agents

Real-world Embabel agent examples distilled from the official docs. See SKILL.md for the core workflow.

## FactChecker — Multi-LLM Fact-Checking with ScatterGather/Consensus

A comprehensive example that demonstrates ScatterGather, ConsensusBuilder, parallel execution, and multi-model fact-checking.

### Key Concepts Demonstrated

- **ConsensusBuilder**: Multiple LLMs extract assertions, then consolidate into unique set
- **ScatterGatherBuilder**: Fan-out to multiple LLMs for fact-checking, then reconcile results
- **@AchievesGoal with @Export(remote=true)**: Auto-publishes as MCP tool
- **ConfigurationProperties**: Type-safe config with PromptContributor factory
- **Parallel execution**: `context.parallelMap()` for concurrent LLM calls
- **Multi-model consensus**: Fact-check across multiple LLMs for higher accuracy

### Full Code

```java
@ConfigurationProperties("embabel.fact-checker")
record FactCheckerProperties(
        int reasoningWordCount,
        List<String> trustedSources,
        List<String> untrustedSources,
        List<String> models,
        String deduplicationModel,
        int maxConcurrency
) {
    LlmOptions deduplicationLlm() {
        return LlmOptions.withModel(deduplicationModel);
    }

    PromptContributor promptContributor() {
        return PromptContributor.fixed(
                """
                        Be guided by the following regarding sources:
                        - Trusted sources: %s
                        - Untrusted sources: %s
                        """.formatted(
                        String.join(", ", trustedSources),
                        String.join(", ", untrustedSources)
                )
        );
    }
}

@Agent(description = "Fact checker agent")
class FactChecker {

    private final FactCheckerProperties properties;

    public FactChecker(FactCheckerProperties properties) {
        this.properties = properties;
    }

    @Action
    DistinctFactualAssertions identifyDistinctFactualAssertions(
            UserInput userInput,
            ActionContext actionContext) {
        return ConsensusBuilder
                .returning(DistinctFactualAssertions.class)
                .sourcedFrom(factualAssertionExtractors(userInput, actionContext).toList())
                .withConsensusBy(this::consolidateFactualAssertions)
                .asSubProcess(actionContext);
    }

    @AchievesGoal(description = "Content has been fact-checked",
            export = @Export(remote = true, startingInputTypes = {UserInput.class}))
    @Action
    FactChecks runAndConsolidateFactChecks(
            DistinctFactualAssertions distinctFactualAssertions,
            ActionContext context) {
        var llmFactChecks = properties.models().stream()
                .flatMap(model -> factCheckWithSingleLlm(model, distinctFactualAssertions, context))
                .toList();
        return ScatterGatherBuilder
                .returning(FactChecks.class)
                .fromElements(FactCheck.class)
                .generatedBy(llmFactChecks)
                .consolidatedBy(this::reconcileFactChecks)
                .asSubProcess(context);
    }

    private Stream<Supplier<FactCheck>> factCheckWithSingleLlm(
            String model,
            DistinctFactualAssertions distinctFactualAssertions,
            OperationContext context) {
        return context.parallelMap(
                distinctFactualAssertions.assertions(),
                properties.maxConcurrency(),
                assertion -> context.ai()
                        .withLlm(LlmOptions.withModel(model).withTimeout(Duration.ofMinutes(3)))
                        .withPromptContributor(properties.promptContributor())
                        .withTools(CoreToolGroups.WEB)
                        .createObject(
                                """
                                        Given the following assertion, check if it is true or false and explain why in %d words
                                        Express your confidence in your determination as a number between 0 and 1.
                                        Use web tools so you can cite information to support your conclusion.
                                        Use '%s' for the source field.

                                        ASSERTION TO CHECK:
                                        %s
                                        """.formatted(
                                        properties.reasoningWordCount(),
                                        model,
                                        assertion
                                ),
                                FactCheck.class
                        )
        ).stream();
    }
}
```

### Data Model

```java
record DistinctFactualAssertions(List<String> assertions) {}

record FactCheck(
        String assertion,
        boolean isTrue,
        @JsonPropertyDescription("confidence in your judgment as to whether the assertion true or false. From 0-1")
        double confidence,
        @JsonPropertyDescription("reasoning for your scoring")
        String reasoning,
        @JsonPropertyDescription("Source of the fact checks, typically a LLM model")
        String source,
        List<InternetResource> links
) {}

record FactChecks(List<FactCheck> checks) {}
```

## StarNewsFinder — Human-in-the-Loop with WaitFor

An agent that combines horoscope reading with news stories, using `WaitFor` for human-in-the-loop interaction.

### Key Concepts Demonstrated

- **WaitFor.formSubmission**: Pauses agent execution for user input
- **@Cost on actions**: Controls planner action selection priority
- **Multiple LLM models**: Different models for different steps
- **Domain model with tools**: `HoroscopeService` exposed as a tool
- **Spring injection**: `@Value` for configuration
- **LlmReference**: Tool-based RAG integration

### Key Pattern

```java
@Action(cost = 100.0) // Make it costly so it won't be used unless there's no other path
public Starry makeStarry(Person person) {
    return WaitFor.formSubmission(
        "Let's get some astrological details for " + person.getName(),
        Starry.class
    );
}
```

High `cost` ensures this action is only used when no cheaper path exists, making it a fallback for human input.