---
name: embabel-agent
description: >-
  Build agentic AI applications on the JVM with Embabel — a Spring-based framework by Rod Johnson for creating agents that mix LLM interactions with code, domain models, and non-LLM planning algorithms (GOAP, Utility AI, Hybrid, Supervisor). Use this skill whenever the user asks about Embabel agent development, agent annotations (@Agent, @Agentic, @Action, @Condition, @AchievesGoal, @State, @EmbabelComponent), agentic tool design (@LlmTool, @Tool, ToolCallContext, tool groups, subagent handoffs), planning configuration, testing with FakePromptRunner or EmbabelMockitoIntegrationTest, LLM provider setup, execution modes (SIMPLE/CONCURRENT), autonomy modes (CLOSED/OPEN), state-based workflows, human-in-the-loop patterns, or project scaffolding with project-creator.sh. Also trigger when the user mentions Embabel, DICE framework, Rod Johnson's agent framework, JVM-based agentic flows, or building agents with Spring Boot and LLMs.
---

# Embabel Agent Framework

This skill helps you build agentic AI applications on the JVM using the **Embabel framework** — a Spring-based framework for authoring agentic flows that seamlessly mix LLM-prompted interactions with code and domain models. Created by Rod Johnson (creator of Spring), it supports intelligent path-finding towards goals using non-LLM AI planning algorithms (GOAP, Utility AI, Hybrid, Supervisor).

## Output Quality

When producing code or documentation:

- **Be comprehensive** — Provide complete, multi-section outputs with thorough explanations, not minimal snippets
- **Include complete code** — Full classes with imports, domain models, and all annotations, not partial examples
- **Show the "why"** — Explain design decisions (why this planner, why this temperature, why this execution mode)
- **Include configuration** — Always show the full `application.yml` block with all relevant settings
- **Provide testing** — Include unit tests with FakePromptRunner AND integration tests with EmbabelMockitoIntegrationTest
- **Use latest API patterns** — `LlmOptions.withModel()`, `context.ai().withLlm().creating().fromPrompt()`, `.withId()`, `withExample()`

## Core Concepts

Every Embabel agent is built from these building blocks:

- **Actions** (`@Action`) — Steps the agent takes. Each method transforms inputs into outputs.
- **Goals** (`@AchievesGoal`) — What the agent is trying to achieve. Marks goal-satisfaction.
- **Conditions** (`@Condition`) — Predicates evaluated before actions or to determine goal achievement.
- **Domain Model** — Strongly-typed objects carrying both data and behavior (DICE).
- **Blackboard** — Shared memory where actions add results and read inputs by type.
- **Plan** — Dynamically generated sequence of actions. The planner recomputes after each action (OODA loop).

> **Key insight:** Application developers rarely interact with the blackboard directly. Most pre/post conditions are inferred from data flow in method signatures.

## Getting Started

### Setup (Maven)

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

Available starters:
- `embabel-agent-starter` — Basic agent platform
- `embabel-agent-starter-shell` — Interactive CLI
- `embabel-agent-starter-mcp` — MCP server

Add Embabel repository for snapshots:
```xml
<repository>
    <id>embabel-snapshots</id>
    <url>https://repo.embabel.com/artifactory/libs-snapshot</url>
</repository>
```

### Quick Start Templates

Use the bundled project-creator script to scaffold a new Embabel project:

```bash
# Java project
scripts/project-creator.sh --lang java --name my-agent

# Kotlin project with custom package
scripts/project-creator.sh --lang kotlin --name research-agent --package com.acme.research
```

The script clones the appropriate template repo and configures the project. It requires `uvx` (Python package manager) and `git`.

Alternatively, clone templates directly:
- [Java Template](https://github.com/embabel/java-agent-template) — Clone and customize
- [Kotlin Template](https://github.com/embabel/kotlin-agent-template) — Clone and customize

### Environment Setup

```
OPENAI_API_KEY=your_key_here
ANTHROPIC_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
```

### LLM Provider Dependencies

| Provider | Dependency | Default LLM |
|----------|-----------|-------------|
| OpenAI | `embabel-agent-starter-openai` | gpt-4o |
| Anthropic | `embabel-agent-starter-anthropic` | claude-sonnet-4-20250514 |
| Google Gemini | `embabel-agent-starter-gemini` | gemini-2.5-flash |
| Google GenAI | `embabel-agent-starter-google-genai` | gemini-3.5-flash |
| DeepSeek | `embabel-agent-starter-deepseek` | deepseek-chat |
| Mistral | `embabel-agent-starter-mistral-ai` | mistral-large |
| LM Studio | `embabel-agent-starter-lmstudio` | local model |
| Ollama | `embabel-agent-starter-ollama` | llama3.3 |
| OCI | `embabel-agent-starter-oci-genai` | oci-genai |

## Agent Authoring Patterns

### Annotation-Based Model (Java/Kotlin)

#### @Agent — Direct Agent

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

#### @Agentic — Auto-Discovered Agent

```java
@Agentic(description = "Generates reports from data")
public class ReportAgent { ... }
```

Both are Spring beans. `@Agentic` agents are auto-registered and picked up by the platform's agent scanning (enabled by default via `embabel.agent.platform.scanning.annotation`).

#### @EmbabelComponent — Action Container (Not an Agent)

```java
@EmbabelComponent
public class IssueActions {
    @Action(cost = 0.1, value = 0.8, outputBinding = "ghIssue")
    public GHIssue saveNewIssue(GHIssue ghIssue, OperationContext context) { ... }
}
```

Useful with Utility AI planner that selects the most valuable next action among all available actions.

### @Action Annotation Details

```java
@Action(
    description = "Search for flights",
    pre = {"spel:destination != null"},
    post = {"flightInfo != null"},
    canRerun = false,
    readOnly = true,
    clearBlackboard = false,
    cost = 0.1,
    value = 0.5
)
public FlightInfo searchFlights(Destination dest, Ai ai) { ... }
```

Key attributes:
- **pre/post** — Additional conditions beyond input types (SpEL or method names)
- **canRerun** — Can the action run again? Defaults to false.
- **readOnly** — No external side effects (useful for learning/catchup modes)
- **clearBlackboard** — Clear blackboard after action, keeping only output. Useful for looping states.
- **cost/value** — For Utility AI planning (0.0–1.0)
- **outputBinding** — Explicit blackboard binding name
- **trigger** — Fire only when a specific type is the most recently added blackboard value

#### Dynamic Cost Computation

```java
@Cost(name = "computeCost")
public double computeCost(@Nullable GHIssue issue, Blackboard bb) {
    return issue != null ? 0.8 : 0.2;
}

@Action(costMethod = "computeCost")
public GHIssue saveIssue(GHIssue input, OperationContext context) { ... }
```

#### @Provided — Inject Platform Beans

```java
@Action
public TicketCategory triage(
        Ticket ticket,
        @Provided TicketFlow flow) {  // injected from Spring context
    return flow.process(ticket);
}
```

Use `@Provided` for services, configuration, or enclosing component references (especially in `@State` classes).

### @Condition Annotation

```java
@Condition
public boolean hasCriticalTicket(Ticket ticket) {
    return ticket.isCritical();
}
```

Conditions should not have side effects — they may be called multiple times.

#### Dynamic SpEL Conditions

```java
@Action(pre = {
    "spel:urgency > 0.5",
    "spel:newEntity.newEntities.?[#this instanceof T(com.example.Issue)].size() > 0"
})
public void handleIssue(GHIssue issue, OperationContext context) { ... }
```

### @SecureAgentTool — Security

```java
@Agent
@SecureAgentTool(expression = "hasAuthority('news:read')")
public class NewsAgent { ... }
```

Method-level annotations override class-level. Requires `embabel-agent-mcp-security` starter.

## Domain Objects

Domain objects carry both data and behavior. Expose methods to LLMs with `@Tool`:

```java
public record Customer(long id, String name, double balance) {
    @Tool
    public double getLoyaltyDiscount() {
        return balance > 1000 ? 0.15 : 0.05;
    }

    // Unannotated methods are never exposed to LLMs
    void updateLoyaltyLevel() { ... }
}
```

Add to prompts:

```java
context.ai().withDefaultLlm()
    .withToolObject(customer)
    .creating(Order.class)
    .fromPrompt("Create an order for this customer");
```

## Tools

### @LlmTool: Expose JVM Methods to LLMs

```java
public class MathTools {
    @LlmTool(description = "add two numbers")
    public double add(double a, double b) { return a + b; }
}
```

### ToolCallContext — Out-of-Band Metadata

```java
@LlmTool(description = "Look up customer")
public String lookupCustomer(long customerId, ToolCallContext context) {
    String tenantId = context.get("tenantId");
    String authToken = context.get("authToken");
    return customerService.lookup(customerId, tenantId, authToken);
}
```

Set at process boundary: `ProcessOptions.withToolCallContext(Map.of("tenantId", "acme"))`
Set per-interaction: `context.ai().withDefaultLlm().withToolCallContext(Map.of("entityId", "123"))`

### Subagent: Agent Handoffs

```java
var subagent = Subagent.ofClass(PerformanceFinder.class)
    .consuming(WorksToFind.class);

context.ai().withDefaultLlm()
    .withTool(subagent)
    .creating(Concert.class)
    .fromPrompt("Find performances and assemble a concert");
```

### Agentic Tools

```java
// Simple: all tools available immediately
var agenticTool = SimpleAgenticTool.builder()
    .withLlm(LlmOptions.withModel("gpt-4o"))
    .withTools(searchTool, analyzeTool, summarizeTool)
    .build();

// Playbook: progressive unlocking
var playbook = PlaybookTool.builder()
    .withTools(searchTool)
    .withTools(analyzeTool, PlaybookCondition.prerequisite("search"))
    .build();

// StateMachine: state-based availability
var sm = StateMachineTool.builder(OrderState.class)
    .withTool(createTool, OrderState.DRAFT)
    .withTool(confirmTool, OrderState.DRAFT, OrderState.CONFIRMED)
    .build();
```

### Tool Groups

```yaml
embabel:
  agent:
    platform:
      tools:
        weather:
          description: Get weather for location
          provider: Docker
          tools: [weather]
```

Use in actions:

```java
context.ai().withDefaultLlm()
    .withToolGroup(CoreToolGroups.WEB)
    .creating(RelevantNews.class)
    .fromPrompt("Find news about " + person.name());
```

### Tool Chaining

```java
context.ai().withDefaultLlm()
    .withToolChainingFrom(Customer.class)
    .creating(Customer.class)
    .fromPrompt("Find the customer");
// After customer is returned, customer.getLoyaltyDiscount() becomes available
```

## Planning Algorithms

| Planner | Best For | Determinism | Algorithm |
|---------|----------|-------------|-----------|
| **GOAP** (default) | Business processes with defined outputs | High | A* search over goal states |
| **Utility** | Exploration, event-driven systems | Medium | Highest net-value action picks |
| **Hybrid** | Reducer pipelines | Medium-High | Utility picking + goal termination |
| **Supervisor** | Flexible multi-step workflows | Low | LLM-orchestrated composition |

Set via `@Agent(planner = PlannerType.XXX)` or `ProcessOptions(plannerType = PlannerType.XXX)`.

### Utility AI

```java
@EmbabelComponent
public class IssueActions {
    @Action(cost = 0.1, value = 0.8)
    public GHIssue saveNewIssue(GHIssue ghIssue, OperationContext context) { ... }
}
```

### States with @State

```java
@State
public sealed interface TicketCategory permits CriticalTicket, BugTicket, GeneralTicket {}

@Action
public TicketCategory triageTicket(Ticket ticket) {
    if (ticket.description().contains("down")) return new CriticalTicket(ticket);
    return new BugTicket(ticket);
}
```

State transitions: previous states hidden, new state bound, only current state's actions considered.

For looping states, use `@Action(clearBlackboard = true)`.

For human-in-the-loop: `WaitFor.formSubmission("Review this", HumanFeedback.class)`

See `reference/states.md` for detailed state patterns.

## Execution Modes

### Process Execution (SIMPLE vs CONCURRENT)

| Mode | Behavior | When to Use |
|------|----------|-------------|
| **SIMPLE** (default) | Sequential: one action at a time | Most agents; predictable; easy to debug |
| **CONCURRENT** | All achievable actions run in parallel | Independent sub-tasks; fan-out/fan-in |

Set: `embabel.agent.platform.process-type: CONCURRENT`

### Autonomy (Closed vs Open Mode)

| Mode | Behavior | When to Use |
|------|----------|-------------|
| **Closed** | LLM picks one agent; agent runs in isolation | Strict agent boundaries |
| **Open** | LLM picks goal; assembles agent from all actions | Maximum flexibility |

```java
// Closed
autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT, agentPlatform, bindings);

// Open
autonomy.chooseAndAccomplishGoal(ProcessOptions.DEFAULT, approver, agentPlatform, bindings);
```

## Invocation

### AgentInvocation (Type-Safe)

```java
var invocation = AgentInvocation.create(agentPlatform, ReviewedStory.class);
ReviewedStory result = invocation.invoke(new UserInput("Tell me a story"));
```

### AgentProcess (Direct)

```java
AgentProcess process = agentPlatform.createAgentProcess(myAgent, options, bindings);
Object result = agentPlatform.start(process).get();
```

### Async

```java
var future = invocation.invokeAsync(input);
CompletableFuture<ReviewedStory> cf = future;
```

### REST Endpoints

- `GET /api/v1/process/{processId}` — Process status
- `DELETE /api/v1/process/{processId}` — Kill process
- `GET /events/process/{processId}` — SSE stream

## LLM Integration

### Mixing Models

```java
var writer = LlmOptions.withModel(OpenAiModels.GPT_4O_MINI)
    .withTemperature(0.8);

var reviewer = LlmOptions.withModel(OpenAiModels.GPT_4O)
    .withTemperature(0.2);
```

### Using the Ai Interface

```java
public record InjectedComponent(Ai ai) {
    public Joke createJoke(String topic1, String topic2, String voice) {
        return ai.withLlm(LlmOptions.withDefaultLlm().withTemperature(.8))
            .withId("tell-joke")
            .creating(Joke.class)
            .fromPrompt("Tell me a joke about %s and %s. Voice: %s".formatted(topic1, topic2, voice));
    }
}
```

### PromptRunner Methods

| Method | Purpose |
|--------|---------|
| `createObject(prompt, Class<T>)` | Create typed object (throws on failure, triggers retry) |
| `createObjectIfPossible(prompt, Class<T>)` | Try to create, return null on failure |
| `generateText(prompt)` | Simple text response |
| `withLlm(LlmOptions)` | Set LLM config |
| `withToolGroup(String)` | Add tool group |
| `withToolObject(obj)` | Add domain object with @Tool methods |
| `withId("id")` | Tag interaction for test verification |
| `withExample(text, obj)` | Few-shot example |
| `withPromptContributor(pc)` | Add prompt content |
| `rendering("template-name")` | Use Jinja template |

### Vision Support

```java
var image = AgentImage.fromFile(new File("diagram.png"));
context.ai().withDefaultLlm()
    .withImage(image)
    .generateText("What is in this image?");
```

### Streaming

```java
context.ai().withDefaultLlm()
    .streaming()
    .createObject(Report.class)
    .fromPrompt("Generate report")
    .doOnNext(event -> {
        if (event instanceof Thinking thinking) { ... }
        else if (event instanceof ObjectCreated obj) { ... }
    })
    .doOnComplete(() -> { ... })
    .subscribe();
```

### Custom LLM Provider

Implement `LlmMessageSender` for unsupported providers:

```java
public class CustomLlmMessageSender implements LlmMessageSender {
    @Override
    public LlmMessageResponse sendMessage(List<Message> messages) {
        // HTTP call to your provider
        return new LlmMessageResponse(message, textContent, usage);
    }
}

@Bean
LlmService customLlm() {
    return new LlmService() {
        @Override public String getName() { return "my-custom-llm"; }
        @Override public LlmMessageSender createMessageSender() { return new CustomLlmMessageSender(); }
    };
}
```

### Callbacks / Interceptors

```java
@Bean
ToolLoopInspector toolLoopLoggingInspector() {
    return new ToolLoopLoggingInspector();
}

@Bean
ToolResultTruncatingTransformer truncatingTransformer() {
    return new ToolResultTruncatingTransformer(5000);
}
```

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
      process-type: SIMPLE  # or CONCURRENT
      toolloop:
        max-iterations: 20
        type: default  # or "parallel"
        empty-response:
          max-retries: 1
      autonomy:
        agent-confidence-cut-off: 0.6
        goal-confidence-cut-off: 0.6
      logging:
        personality: starwars  # starwars, severance, colossus, hitchhiker, montypython
        include-plan: true
        level: DEBUG
      rest:
        enabled: true
```

> **Important:** When producing configuration examples, always include the complete `embabel:` block with models, planner settings, execution mode, logging, and tool configuration — not just a snippet.

See `reference/configuration.md` for full property reference and provider-specific config.

## Testing

Embabel provides first-class testing support. Always provide **complete, runnable test classes**.

### Unit Testing with FakePromptRunner

```java
class StoryWriterTest {
    @Test
    void writeStory_promptContainsTopic() {
        var context = FakeOperationContext.create();
        var promptRunner = (FakePromptRunner) context.promptRunner();
        context.expectResponse(new Story("Once upon a time..."));

        var agent = new WriteAndReviewAgent();
        var story = agent.writeStory(new UserInput("space exploration"), context);

        assertEquals(new Story("Once upon a time..."), story);
        var prompt = promptRunner.getLlmInvocations().getFirst().getPrompt();
        assertTrue(prompt.contains("space exploration"));
    }

    @Test
    void writeStory_temperatureIs0_8() {
        var context = FakeOperationContext.create();
        context.expectResponse(new Story("..."));

        var agent = new WriteAndReviewAgent();
        agent.writeStory(new UserInput("test"), context);

        var temperature = promptRunner.getLlmInvocations().getFirst()
            .getInteraction().getLlm().getTemperature();
        assertEquals(0.8, temperature, 0.01);
    }
}
```

### Integration Testing

```java
class StoryWriterIntegrationTest extends EmbabelMockitoIntegrationTest {
    @Test
    void shouldExecuteCompleteWorkflow() {
        whenCreateObject(contains("Craft a short story"), Story.class).thenReturn(story);
        whenGenerateText(contains("Review this story")).thenReturn(reviewedStory.review());

        var invocation = AgentInvocation.create(agentPlatform, ReviewedStory.class);
        var result = invocation.invoke(input);

        assertNotNull(result);
        verifyCreateObjectMatching(
            prompt -> prompt.contains("Craft a short story"),
            Story.class,
            llm -> llm.getLlm().getTemperature() == 0.9
        );
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

See `reference/testing.md` for more patterns.

## Reference Files

- `reference/configuration.md` — Full configuration property reference, provider-specific setup, custom LLM integration
- `reference/testing.md` — Testing patterns and examples
- `reference/planners.md` — Detailed planner guide (GOAP, Utility AI, Hybrid, Supervisor)
- `reference/states.md` — State patterns, looping, human-in-the-loop, WaitFor
- `reference/tools.md` — Tools, tool groups, ToolCallContext, subagents, agentic tools
- `reference/invocation.md` — Invocation patterns, Autonomy, REST endpoints, webhooks
- `reference/guide-server.md` — Guide server setup, MCP client integrations, WebSocket chat API, Docker deployment

## Scaffolding

When the user wants to create a new Embabel project from scratch, use the bundled project-creator script:

```bash
# Java project (default)
scripts/project-creator.sh --name my-agent

# Kotlin project
scripts/project-creator.sh --lang kotlin --name my-agent

# Custom package name
scripts/project-creator.sh --name my-agent --package com.example.myagent
```

The script:
- Clones the Embabel project-creator tool
- Fetches the Java or Kotlin template repository
- Generates a complete project with Maven/Gradle, Spring Boot, and Embabel dependencies
- Sets up the `WriteAndReviewAgent` example

After scaffolding, the user can:
- Run the shell: `./mvnw spring-boot:run` (or `./gradlew bootRun`)
- Customize the agent in the generated code
- Add more agents, tools, and planners as needed

> **When to use:** New projects, quick prototypes, or when the user says "create a new Embabel project", "scaffold an Embabel app", or "generate an Embabel template".

## Common Pitfalls

1. **Forgetting `@Agent` or `@Agentic` on the class** — The agent won't be discovered by the platform
2. **Not providing `OperationContext`** — Actions need it to access the AI and blackboard
3. **Confusing execution modes** — SIMPLE vs CONCURRENT (process execution) vs CLOSED vs OPEN (autonomy)
4. **Not using `@AchievesGoal`** — Without it, the planner can't determine goal satisfaction
5. **Ignoring tool loop iterations** — Default is 20; increase for complex multi-step agents
6. **Not setting model per-action** — Using the default model for everything wastes money or sacrifices quality
7. **Forgetting `.withId()` on LLM calls** — Makes test verification harder and debugging opaque
8. **Using non-static inner classes for @State** — Causes serialization/persistence issues; use records (Java) or top-level classes (Kotlin)
9. **Not using `clearBlackboard = true` for looping states** — Planner sees output type already exists and skips
10. **Allowing LLMs to call sensitive methods** — Always gate with `@Tool`/`@LlmTool`; unannotated methods stay hidden
