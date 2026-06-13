# DSL Builders Reference

Embabel provides DSL builders for creating agents using workflows where you want an atomic action that is complete in itself but may contain multiple steps.

## Standard Workflows

### SimpleAgentBuilder

The simplest agent, with no preconditions or postconditions:

```java
var agent = SimpleAgentBuilder.builder(String.class)
    .withAction(ctx -> ctx.ai().withDefaultLlm()
        .creating(String.class)
        .fromPrompt(ctx.getInput()))
    .buildAgent("simple-agent", "Simple text generation");
```

### ScatterGatherBuilder

Fork-join pattern for parallel processing:

```java
var agent = ScatterGatherBuilder.builder(String.class, FactCheck.class)
    .withForks(List.of(factCheck1, factCheck2, factCheck3))
    .withGather(facts -> new FactChecks(facts))
    .buildAgent("fact-checker", "Fact check from multiple sources");
```

### ConsensusBuilder

A pattern for reaching consensus among multiple sources (specialization of ScatterGather):

```java
var agent = ConsensusBuilder.builder(String.class, Opinion.class)
    .withSources(opinionSources)
    .withConsensus(opinions -> reachConsensus(opinions))
    .buildAgent("consensus", "Reach consensus from sources");
```

### RepeatUntil

Repeats a step until a condition is met:

```java
var agent = RepeatUntil.builder(String.class)
    .withStep(ctx -> generateDraft(ctx.getInput()))
    .withCondition(draft -> draft.isAcceptable())
    .buildAgent("draft-until-acceptable", "Iterative draft refinement");
```

### RepeatUntilAcceptable

Repeats a step while a condition is met, with a separate evaluator providing feedback:

```java
var agent = RepeatUntilAcceptable.builder(String.class)
    .withStep(ctx -> refineDraft(ctx.getInput()))
    .withEvaluator(draft -> evaluateQuality(draft))
    .buildAgent("refine-until-good", "Refine until quality threshold");
```

## Running as Subprocess

DSL builders can run as subprocesses of the current process:

```java
var result = agent.asSubProcess(input, context);
```

## Registering Agent Beans

Whereas the `@Agent` annotation causes a class to be picked up immediately by Spring, with the DSL you'll need an extra step to register an agent with Spring:

```java
@Configuration
public class AgentConfig {
    @Bean
    public Agent factCheckerAgent() {
        return ScatterGatherBuilder.builder(String.class, FactCheck.class)
            .withForks(List.of(factCheck1, factCheck2))
            .withGather(facts -> new FactChecks(facts))
            .buildAgent("fact-checker", "Fact check from multiple sources");
    }
}
```

Any `@Bean` of `Agent` type results in auto-registration, just like declaring a class annotated with `@Agent`.

## Key Points

- DSL builders create atomic, self-contained workflows
- All builders are type-safe
- Use `asSubProcess()` to run within another agent's execution
- Register with Spring via `@Bean` method returning `Agent`
- Choose the builder that matches your workflow pattern
- The embabel-agent-examples repository includes a fact checker using ScatterGather
