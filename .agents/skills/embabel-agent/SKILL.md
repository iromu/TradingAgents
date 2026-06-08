---
name: embabel-agent
description: Build agentic AI applications on the JVM with Embabel — a Spring-based framework for creating agents that mix LLM interactions with code and planning. Use this skill when building agents with @Agent or @Agentic annotations, configuring GOAP or Utility AI planning, writing tests with FakePromptRunner, setting up @LlmTool or @Condition, mixing LLM models, or configuring Embabel's execution modes (FOCUSED, CLOSED, OPEN). Also trigger for Embabel application.yml configuration, Spring Boot integration with AI agents, setting up the Embabel Guide server (MCP/chat/RAG), configuring MCP client integrations (Claude Desktop, Claude Code, Cursor, Codex), or when the user mentions Embabel, Rod Johnson's agent framework, JVM-based agentic flows, or the Embabel documentation server.
---

# Embabel Agent Framework

This skill helps you build agentic AI applications on the JVM using the **Embabel framework** — a framework for
authoring agentic flows that seamlessly mix LLM-prompted interactions with code and domain models. Created by Rod
Johnson (creator of Spring), it supports intelligent path-finding towards goals using non-LLM AI planning algorithms.

## Output Quality

When producing code or documentation:

- **Be comprehensive** — Provide complete, multi-section outputs with thorough explanations, not minimal snippets
- **Include complete code** — Full classes with imports, domain models, and all annotations, not partial examples
- **Show the "why"** — Explain design decisions (why this planner, why this temperature, why this execution mode)
- **Include configuration** — Always show the full `application.yml` block with all relevant settings
- **Provide testing** — Include unit tests with FakePromptRunner AND integration tests with
  EmbabelMockitoIntegrationTest
- **Use latest API patterns** — `LlmOptions.withModel()`, `context.ai().withLlm().creating().fromPrompt()`, `.withId()`,
  `CreationExample`

## Core Concepts

Every Embabel agent is built from these building blocks:

- **Actions** (`@Action`) — Steps the agent takes to make progress. Each action is a method that transforms inputs into
  outputs.
- **Goals** (`@AchievesGoal`) — What the agent is trying to achieve. Goals define success conditions.
- **Conditions** (`@Condition`) — Predicates evaluated before actions or to determine goal achievement. Reassessed after
  every action.
- **Domain Model** — The objects underpinning the flow, carrying both data and behavior.
- **Plan** — A dynamically generated sequence of actions (not hand-coded). The planner recomputes the plan after each
  action, forming an OODA loop.

> **Key insight:** Application developers rarely deal with these concepts directly. Most pre/post conditions are
> inferred from data flow in code.

## Agent Authoring Patterns

### Annotation-Based Model (Java/Kotlin)

Use `@Agent` for agents you call directly, or `@Agentic` for agents the platform auto-discovers and selects.

```java
@Agent(description = "Writes and reviews stories")
public class WriteAndReviewAgent {

    @Action
    public Story writeStory(UserInput input, OperationContext context) {
        var writer = LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
            .withTemperature(0.8)
            .withPersona("You are a creative storyteller");
        return context.ai()
            .withLlm(writer)
            .withId("write-story")
            .creating(Story.class)
            .fromPrompt("Write a story about: " + input.getContent());
    }

    @AchievesGoal(description = "Review and improve the story")
    @Action
    public ReviewedStory reviewStory(Story story, OperationContext context) {
        var reviewer = LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
            .withTemperature(0.2)
            .withPersona("You are a careful editor");
        return context.ai()
            .withLlm(reviewer)
            .withId("review-story")
            .creating(ReviewedStory.class)
            .fromPrompt("Review this story and suggest improvements: " + story.text());
    }
}
```

### Kotlin DSL Model

```kotlin
val agent = Agent(
    name = "my-agent",
    goals = setOf(
        Goal(name = "produce-report", description = "Generate a report")
    )
) {
    action {
        // action definition
    }
}
```

### `@Agent` vs `@Agentic`

| Annotation | When to use                                                        | Discovery                        |
|------------|--------------------------------------------------------------------|----------------------------------|
| `@Agent`   | You call the agent directly from code (Focused mode)               | Explicit by name                 |
| `@Agentic` | The platform should auto-discover and optionally select this agent | Auto-registered as a Spring bean |

Both are Spring beans. `@Agentic` agents are picked up by agent scanning automatically (enabled by default via
`embabel.agent.platform.scanning.annotation`).

## Planning Algorithms

Choose a planner based on your use case:

| Planner            | Best For                                    | Behavior                                                                               |
|--------------------|---------------------------------------------|----------------------------------------------------------------------------------------|
| **GOAP** (default) | Business processes with defined outputs     | Goal-oriented, deterministic planning using A* search. Finds optimal action sequences. |
| **Utility AI**     | Exploration, event-driven systems, chatbots | Selects highest net-value action (`value - cost`) at each step.                        |
| **Hybrid**         | Reducer pipelines                           | Utility picking + goal-satisfaction termination.                                       |
| **Supervisor**     | Flexible multi-step workflows               | LLM-orchestrated, non-deterministic composition.                                       |

Set via `@Agent(planner = PlannerType.XXX)` or `ProcessOptions(plannerType = PlannerType.XXX)`.

### Utility AI: Cost, Value, and States

Utility agents use `@EmbabelComponent` for action classes (not `@Agent`). Each action declares its `cost` and `value`.
The planner picks the action with the highest net value.

```java
@EmbabelComponent
public class IssueActions {

    @Action(cost = 0.1, value = 0.8, outputBinding = "ghIssue")
    public GHIssue saveNewIssue(GHIssue ghIssue, OperationContext context) { ... }

    @Action(pre = {"spel:newEntity.newEntities.?[#this instanceof T(com.embabel.shepherd.domain.Issue)].size() > 0"})
    public IssueAssessment reactToNewIssue(GHIssue ghIssue, NewEntity<?> newEntity, Ai ai) { ... }
}
```

Use `@State` sealed interfaces to model state transitions:

```java
@Agent(description = "Triage support tickets", planner = PlannerType.UTILITY)
public class TicketTriageAgent {

    @State
    public sealed interface TicketCategory permits CriticalTicket, BugTicket, GeneralTicket {}

    @Action
    public TicketCategory triageTicket(Ticket ticket) {
        if (ticket.description().toLowerCase().contains("down")) {
            return new CriticalTicket(ticket);
        }
        // ...
    }

    @State
    public record CriticalTicket(Ticket ticket) implements TicketCategory {
        @AchievesGoal(description = "Handle critical ticket")
        @Action
        public ResolvedTicket handleCritical() { ... }
    }
}
```

## Execution Modes

| Mode        | Behavior                                                         | When to Use                                                                                    |
|-------------|------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| **Focused** | User code calls a specific agent with specific input             | Code-driven flows, event-triggered agents                                                      |
| **Closed**  | Platform selects an agent based on intent classification         | When you have multiple agents and want the LLM to pick the right one                           |
| **Open**    | Platform uses all available goals/actions to achieve user intent | Maximum flexibility, least deterministic. The agent assembles itself from available resources. |

```java
// Focused
AgentProcess process = agentPlatform.createAgentProcess(myAgent, options, bindings);

// Closed (LLM picks agent)
autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT);

// Open (LLM picks goal + assembles agent)
autonomy.chooseAndAccomplishGoal(options, approver, agentPlatform, bindings, selectionOptions);
```

## Tools and MCP Integration

### @LlmTool: Expose JVM Methods to LLMs

```java
public class MathTools {
    @LlmTool(description = "add two numbers")
    public double add(double a, double b) {
        return a + b;
    }
}
```

Tools can be stateful — they often encapsulate domain objects with private state.

### Tool Groups

```java
@Action
public RelevantNews findNews(StarPerson person, OperationContext context) {
    return context.ai().withDefaultLlm()
        .withToolGroup(CoreToolGroups.WEB)
        .createObject(prompt, RelevantNews.class);
}
```

Configure tool groups in `application.yml`:

```yaml
embabel:
  agent:
    platform:
      tools:
        includes:
          weather:
            description: Get weather for location
            provider: Docker
            tools:
              - weather
```

### ToolCallContext

For infrastructure metadata (auth tokens, tenant IDs) that the LLM should never see:

```java
@LlmTool(description = "Look up customer")
public String lookupCustomer(
        @LlmTool.Param(description = "Customer ID") long customerId,
        ToolCallContext context) {
    String tenantId = context.get("tenantId");
    String authToken = context.get("authToken");
    return customerService.lookup(customerId, tenantId, authToken);
}
```

### MCP Server (Guide)

The [Embabel Guide](https://github.com/embabel/guide) is a companion server that exposes Embabel documentation and API
information as MCP tools. Run it alongside your development workflow to let AI coding assistants query Embabel docs via
MCP.

```bash
# Run the Guide server
export OPENAI_API_KEY=your_key_here
./mvnw spring-boot:run

# Exposes MCP tools at http://localhost:1337/sse
```

Integrate with MCP clients:

```bash
# Claude Code
claude mcp add embabel --transport sse http://localhost:1337/sse

# Or add to Claude Desktop config (claude_desktop_config.json):
# {
#   "mcpServers": {
#     "embabel-dev": {
#       "command": "npx",
#       "args": ["-y", "mcp-remote", "http://localhost:1337/sse", "--transport", "sse-only"]
#     }
#   }
# }
```

See `reference/guide-server.md` for full Guide server setup, Docker deployment, and all client integrations (Cursor,
Codex, Antigravity, Copilot CLI).

## Skills System

Embabel implements the [Agent Skills](https://agentskills.io/home) specification:

```kotlin
val skills = Skills(name = "my-skills", description = "Skills for my agent")
    .withLocalSkills("/path/to/skills")
    .withGitHubUrl("https://github.com/anthropics/skills/tree/main/skills")
    .withScriptExecutionEngine(dockerEngine)
```

SKILL.md structure:
```
my-skill/
├── SKILL.md        # Required: name, description, instructions
├── scripts/        # Optional: executable scripts (Python, Bash, etc.)
├── references/     # Optional: docs loaded into context as needed
└── assets/         # Optional: files used in output
```

Script execution supports Process (direct) and Docker (sandboxed) engines.

## LLM Integration

### Mixing Models

```java
// Cheap model for simple tasks
var writer = LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
    .withTemperature(0.8);

// Expensive model for complex reasoning
var reviewer = LlmOptions.withModel(OpenAiModels.GPT_4O)
    .withTemperature(0.2);
```

### Using the Ai Interface

```java
@Component
public record InjectedComponent(Ai ai) {
    public String tellJoke(String topic) {
        return ai.withDefaultLlm().generateText("Tell me a joke about " + topic);
    }

    public Joke createJoke(String topic1, String topic2, String voice) {
        return ai.withLlm(LlmOptions.withDefaultLlm().withTemperature(.8))
            .withId("tell-joke")
            .creating(Joke.class)
            .fromPrompt("Tell me a joke about %s and %s. Voice: %s".formatted(topic1, topic2, voice));
    }
}
```

### Local Models

- **Ollama**: Add `embabel-agent-starter-ollama`
- **Docker**: Add `embabel-agent-starter-dockermodels`
- **LMStudio**: Uses OpenAI-compatible client

## Configuration

Key properties in `application.yml`:

```yaml
embabel:
  models:
    default-llm: gpt-4.1-mini
    llms:
      cheapest: gpt-4o-mini
      best: gpt-4o
      reasoning: o1-preview
  agent:
    platform:
      name: my-agent-platform
      toolloop:
        max-iterations: 20
        type: default  # or "parallel" for experimental parallel tool execution
        empty-response:
          max-retries: 1  # for weak open-weights chat models
      planner:
        type: GOAP
        aStar:
          max-open-list-size: 1000
          max-closed-list-size: 5000
      execution:
        mode: FOCUSED  # FOCUSED, CLOSED, or OPEN
      autonomy:
        agent-confidence-cut-off: 0.6
        goal-confidence-cut-off: 0.6
      logging:
        personality: starwars  # starwars, severance, colossus, hitchhiker, montypython
        include-plan: true
        include-actions: true
        level: DEBUG  # for development
      process-repository:
        enabled: true  # persist process state
      rest:
        enabled: true  # expose REST endpoints
```

> **Important:** When producing configuration examples, always include the complete `embabel:` block with models,
> planner settings, execution mode, logging, and tool configuration — not just a snippet.

## Invocation and Runtime

### Programmatic Invocation

```java
// With AgentProcess
AgentProcess process = agentPlatform.createAgentProcess(myAgent, options, bindings);
Object result = agentPlatform.start(process).get();

// With AgentInvocation (simpler)
var invocation = AgentInvocation.create(agentPlatform, TravelPlan.class);
TravelPlan plan = invocation.invoke(travelRequest);

// Async
CompletableFuture<TravelPlan> future = invocation.invokeAsync(travelRequest);
```

### REST Endpoints

Embabel exposes REST endpoints out of the box:
- `GET /api/v1/process/{processId}` — Process status
- `DELETE /api/v1/process/{processId}` — Kill process
- `GET /events/process/{processId}` — SSE stream

## Getting Started

### Setup (Maven)

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

Add repositories:
```xml
<repositories>
    <repository>
        <id>embabel-releases</id>
        <url>https://repo.embabel.com/artifactory/libs-release</url>
    </repository>
    <repository>
        <id>embabel-snapshots</id>
        <url>https://repo.embabel.com/artifactory/libs-snapshot</url>
    </repository>
</repositories>
```

### Quick Start Templates

- [Java Template](https://github.com/embabel/java-agent-template) — Clone and customize
- [Kotlin Template](https://github.com/embabel/kotlin-agent-template) — Clone and customize
- [Project Creator](https://github.com/embabel/project-creator) —
  `uvx --from git+https://github.com/embabel/project-creator.git project-creator`

## Testing

Embabel provides first-class testing support. Always provide **complete, runnable test classes** — not just snippets.

### Unit Testing with FakePromptRunner

```java
class StoryWriterTest {

    record ContextAndRunner(FakeOperationContext context, FakePromptRunner promptRunner) {
        static ContextAndRunner create() {
            var ctx = FakeOperationContext.create();
            return new ContextAndRunner(ctx, (FakePromptRunner) ctx.promptRunner());
        }
    }

    @Test
    void writeStory_promptContainsTopic() {
        var (context, promptRunner) = ContextAndRunner.create();
        var expectedStory = new Story("Once upon a time...");
        context.expectResponse(expectedStory);

        var agent = new WriteAndReviewAgent();
        var story = agent.writeStory(new UserInput("space exploration"), context);

        assertEquals(expectedStory, story);
        var prompt = promptRunner.getLlmInvocations().getFirst().getPrompt();
        assertTrue(prompt.contains("space exploration"));
    }

    @Test
    void writeStory_temperatureIs0_8() {
        var (context, promptRunner) = ContextAndRunner.create();
        context.expectResponse(new Story("..."));

        var agent = new WriteAndReviewAgent();
        agent.writeStory(new UserInput("test"), context);

        var temperature = promptRunner.getLlmInvocations().getFirst()
            .getInteraction().getLlm().getTemperature();
        assertEquals(0.8, temperature, 0.01);
    }

    @Test
    void writeStory_interactionIdIsSet() {
        var (context, promptRunner) = ContextAndRunner.create();
        context.expectResponse(new Story("..."));

        var agent = new WriteAndReviewAgent();
        agent.writeStory(new UserInput("test"), context);

        var id = promptRunner.getLlmInvocations().getFirst()
            .getInteraction().getId().getValue();
        assertEquals("write-story", id);
    }
}
```

### Key Testing Patterns

1. **`FakeOperationContext.create()`** — Creates a test context with a FakePromptRunner
2. **`context.expectResponse(T)`** — Pre-loads a response for the next LLM call (call in order for multi-step agents)
3. **`context.ai().withId("interaction-id")`** — Set interaction IDs for verification (always use this!)
4. **`promptRunner.getLlmInvocations()`** — Access all LLM calls for verification
5. **`withExample(...)`** — Add few-shot examples to prompts
6. **`fromTemplate("template-name", Map.of(...))`** — Use Jinja templates
7. **`CreationExample`** — Create reusable example definitions for prompts

### Integration Testing

```java
class StoryWriterIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Test
    void shouldExecuteCompleteWorkflow() {
        var input = new UserInput("Write about AI");
        var story = new Story("AI will transform...");
        var reviewed = new ReviewedStory(story, "Excellent.", Personas.REVIEWER);

        whenCreateObject(contains("Craft a short story"), Story.class).thenReturn(story);
        whenGenerateText(contains("Review this story")).thenReturn(reviewed.review());

        var invocation = AgentInvocation.create(agentPlatform, ReviewedStory.class);
        var result = invocation.invoke(input);

        assertNotNull(result);
        verifyCreateObjectMatching(
            prompt -> prompt.contains("Craft a short story"),
            Story.class,
            llm -> llm.getLlm().getTemperature() == 0.9
        );
        verifyNoMoreInteractions();
    }
}
```

### Testing Checklist

- [ ] Verify prompt content contains expected keywords/variables
- [ ] Verify LLM hyperparameters (temperature, model)
- [ ] Set and verify interaction IDs using `.withId("...")`
- [ ] For multi-step agents: call `expectResponse()` for each expected LLM call in order
- [ ] Verify the number of LLM invocations
- [ ] Include integration tests with `EmbabelMockitoIntegrationTest` for full workflow
- [ ] Test plan variations (GOAP selects different actions based on input state)

See `reference/testing.md` for more detailed patterns.

## Reference Files

- `reference/planners.md` — Detailed planner guide (GOAP, Utility AI, Hybrid, Supervisor)
- `reference/testing.md` — Testing patterns and examples
- `reference/configuration.md` — Full configuration property reference
- `reference/guide-server.md` — Guide server setup, MCP client integrations, WebSocket chat API, and Docker deployment

## Common Pitfalls

1. **Forgetting `@Agent` or `@Agentic` on the class** — The agent won't be discovered by the platform
2. **Not providing `OperationContext`** — Actions need it to access the AI and blackboard
3. **Confusing execution modes** — Focused calls a specific agent; Open assembles one dynamically
4. **Not using `@AchievesGoal`** — Without it, the planner can't determine goal satisfaction
5. **Ignoring tool loop iterations** — Default is 20; increase for complex multi-step agents
6. **Not setting model per-action** — Using the default model for everything wastes money or sacrifices quality
7. **Forgetting `.withId()` on LLM calls** — Makes test verification harder and debugging opaque
