# Testing

Embabel provides comprehensive testing support for both unit and integration testing scenarios.

> **IMPORTANT:** Building Gen AI applications is no different from building other software. Testing is critical to delivering quality software and must be considered from the outset.

---

## Unit Testing with FakePromptRunner

Unit testing in Embabel enables testing individual agent actions without involving real LLM calls. Agents are POJOs that can be instantiated with fake or mock objects. Actions are methods that can be called directly with test fixtures.

The framework provides `FakePromptRunner` and `FakeOperationContext` to mock LLM interactions while allowing you to verify prompts, hyperparameters, and business logic. Alternatively, use [Mockito](https://site.mockito.org/) (Java) or [mockk](https://mockk.io/) (Kotlin).

**Kotlin notes:** Use `as FakePromptRunner` for casts, `first()` instead of `getFirst()`, and `llmInvocations` (no `get`) for property access.

### Basic Unit Test with FakeOperationContext

Create a `FakeOperationContext`, preload expected responses, call agent methods, then verify LLM invocations.

```java
@Test
void testCraftStory() {
    var context = FakeOperationContext.create();
    var promptRunner = (FakePromptRunner) context.promptRunner();
    context.expectResponse(new Story("Once upon a time, Sir Galahad..."));

    var agent = new WriteAndReviewAgent(200, 400);
    agent.craftStory(
        new UserInput("Tell me a story about a brave knight", Instant.now()),
        context
    );

    String prompt = promptRunner.getLlmInvocations().getFirst().getPrompt();
    assertTrue(prompt.contains("knight"), "Expected prompt to contain 'knight'");
}
```

```kotlin
@Test
fun testCraftStory() {
    val context = FakeOperationContext.create()
    val promptRunner = context.promptRunner() as FakePromptRunner
    context.expectResponse(Story("Once upon a time, Sir Galahad..."))

    val agent = WriteAndReviewAgent(200, 400)
    agent.craftStory(
        UserInput("Tell me a story about a brave knight", Instant.now()),
        context
    )

    val prompt = promptRunner.llmInvocations.first().prompt
    assertTrue(prompt.contains("knight"), "Expected prompt to contain 'knight'")
}
```

### Verifying Prompts Contain Expected Content

Access the prompt text via `getLlmInvocations()` and assert on its content.

```java
@Test
void testReview() {
    var agent = new WriteAndReviewAgent(200, 400);
    var userInput = new UserInput("Tell me a story about a brave knight", Instant.now());
    var story = new Story("Once upon a time, Sir Galahad...");
    var context = FakeOperationContext.create();
    context.expectResponse("A thrilling tale of bravery and adventure!");
    agent.reviewStory(userInput, story, context);

    var promptRunner = (FakePromptRunner) context.promptRunner();
    String prompt = promptRunner.getLlmInvocations().getFirst().getPrompt();
    assertTrue(prompt.contains("knight"), "Expected review prompt to contain 'knight'");
    assertTrue(prompt.contains("review"), "Expected review prompt to contain 'review'");
}
```

### Verifying LLM Hyperparameters (temperature, model, etc.)

Each `LlmInvocation` carries an `Interaction` with the LLM configuration.

```java
@Test
void testTemperature() {
    var context = FakeOperationContext.create();
    context.expectResponse(new Story("Once upon a time..."));

    var agent = new WriteAndReviewAgent(200, 400);
    agent.craftStory(new UserInput("Tell me a story", Instant.now()), context);

    var temp = context.getLlmInvocations().getFirst()
        .getInteraction().getLlm().getTemperature();
    assertEquals(0.9, temp, 0.01, "Expected temperature 0.9 for creative output");
}
```

```kotlin
@Test
fun testTemperature() {
    val context = FakeOperationContext.create()
    context.expectResponse(Story("Once upon a time..."))

    val agent = WriteAndReviewAgent(200, 400)
    agent.craftStory(UserInput("Tell me a story", Instant.now()), context)

    val temp = context.llmInvocations.first().interaction.llm.temperature
    assertEquals(0.9, temp, 0.01, "Expected temperature 0.9 for creative output")
}
```

### Testing Multiple LLM Interactions

For agents that make several LLM calls, call `expectResponse()` for each call in order.

```java
@Test
void testMultipleInteractions() {
    var input = new UserInput("Write about space exploration");
    var story = new Story("The astronaut gazed at Earth...");
    var review = new ReviewedStory("Compelling narrative.", Personas.REVIEWER);

    context.expectResponse(story);
    context.expectResponse(review);

    var writtenStory = agent.writeStory(input, context);
    var reviewedStory = agent.reviewStory(writtenStory, context);

    assertEquals(story, writtenStory);
    assertEquals(review, reviewedStory);

    var invocations = context.getLlmInvocations();
    assertEquals(2, invocations.size());

    var writerCall = invocations.get(0);
    assertEquals(0.8, writerCall.getInteraction().getLlm().getTemperature(), 0.01);

    var reviewerCall = invocations.get(1);
    assertEquals(0.2, reviewerCall.getInteraction().getLlm().getTemperature(), 0.01);
}
```

### Testing Tools and Tool Groups

```java
// Access tools added via withToolObject() or withTool()
var tools = interaction.getTools();

// Access named tool group requirements added via withToolGroup()
var toolGroups = interaction.getToolGroups();
```

---

## Testing the Fluent API

`FakePromptRunner` fully supports the fluent API patterns used in production code.

### Testing withId() for Interaction Tracing

The `withId()` method sets an interaction ID for better log tracing. In tests, verify the ID was set:

```java
@Test
void shouldSetInteractionIdCorrectly() {
    var context = FakeOperationContext.create();
    var expectedIntent = new UserIntent("command", "Change channel names");
    context.expectResponse(expectedIntent);

    var result = context.ai()
            .withId("classify-intent")
            .creating(UserIntent.class)
            .fromPrompt("Classify the user's intent");

    assertEquals(expectedIntent, result);

    var interaction = context.getLlmInvocations().getFirst().getInteraction();
    assertEquals("classify-intent", interaction.getId().getValue());
}
```

```kotlin
@Test
fun `should set interaction ID correctly`() {
    val context = FakeOperationContext.create()
    val expectedIntent = UserIntent("command", "Change channel names")
    context.expectResponse(expectedIntent)

    val result = context.ai()
        .withId("classify-intent")
        .creating(UserIntent::class.java)
        .fromPrompt("Classify the user's intent")

    assertEquals(expectedIntent, result)

    val interaction = context.llmInvocations.first().interaction
    assertEquals(InteractionId("classify-intent"), interaction.id)
}
```

### Testing creating() with withExample()

The `creating()` API allows you to provide strongly-typed examples to improve LLM output quality. In tests, verify examples were included as prompt contributors:

```java
@Test
void shouldIncludeExamplesInPrompt() {
    var context = FakeOperationContext.create();
    var expectedPlan = new ChannelEditPlan(1, "Lead Vox");
    context.expectResponse(expectedPlan);

    var result = context.ai()
            .withLlm(llmSelectionService.selectOptimalLlm())
            .withId("analyze-edit-request")
            .creating(ChannelEditPlan.class)
            .withExample("Rename channel 1", new ChannelEditPlan(1, "Bass"))
            .withExample("Rename channel 2", new ChannelEditPlan(2, "Drums"))
            .fromPrompt("Analyze the edit request");

    assertEquals(expectedPlan, result);

    var promptContributors = context.getLlmInvocations().getFirst()
            .getInteraction().getPromptContributors();
    assertTrue(promptContributors.size() >= 2, "Examples should be added as prompt contributors");
}
```

### Using CreationExample for Reusable Examples

```java
@Test
void shouldUseCreationExampleDataClass() {
    var context = FakeOperationContext.create();
    var expectedPlan = new ChannelEditPlan(1, "Lead Vox");
    context.expectResponse(expectedPlan);

    var example = new CreationExample<>(
        "Rename channel example",
        new ChannelEditPlan(2, "Rhythm")
    );

    var result = context.ai()
            .withDefaultLlm()
            .creating(ChannelEditPlan.class)
            .withExample(example)
            .fromPrompt("Analyze the edit request");

    assertEquals(expectedPlan, result);
}
```

```kotlin
@Test
fun `should use CreationExample data class`() {
    val context = FakeOperationContext.create()
    val expectedPlan = ChannelEditPlan(1, "Lead Vox")
    context.expectResponse(expectedPlan)

    val example = CreationExample(
        description = "Rename channel example",
        value = ChannelEditPlan(2, "Rhythm")
    )

    val result = context.ai()
        .withDefaultLlm()
        .creating(ChannelEditPlan::class.java)
        .withExample(example)
        .fromPrompt("Analyze the edit request")

    assertEquals(expectedPlan, result)
}
```

### Adding Multiple Examples with withExamples()

```java
@Test
void shouldAddMultipleExamplesFromList() {
    var context = FakeOperationContext.create();
    var expectedPlan = new ChannelEditPlan(1, "Lead Vox");
    context.expectResponse(expectedPlan);

    var examples = List.of(
        new CreationExample<>("Rename to Bass", new ChannelEditPlan(1, "Bass")),
        new CreationExample<>("Rename to Drums", new ChannelEditPlan(2, "Drums")),
        new CreationExample<>("Rename to Keys", new ChannelEditPlan(3, "Keys"))
    );

    var result = context.ai()
            .withDefaultLlm()
            .creating(ChannelEditPlan.class)
            .withExamples(examples)
            .fromPrompt("Analyze the request");

    assertEquals(expectedPlan, result);

    var promptContributors = context.getLlmInvocations().getFirst()
            .getInteraction().getPromptContributors();
    assertTrue(promptContributors.size() >= 3);
}
```

### Full Fluent API Chain Example

```java
@Test
void shouldTestCompleteFluentApiChain() {
    var context = FakeOperationContext.create();
    var expectedOutput = new ComplexOutput("analysis complete", 42);
    context.expectResponse(expectedOutput);

    var result = context.ai()
            .withLlm(LlmOptions.withModel("gpt-4"))
            .withId("complex-analysis")
            .withSystemPrompt("You are an expert analyst")
            .creating(ComplexOutput.class)
            .withExample("Simple case", new ComplexOutput("basic", 1))
            .withExample("Complex case", new ComplexOutput("advanced", 100))
            .fromPrompt("Analyze the input data");

    assertEquals(expectedOutput, result);

    var invocation = context.getLlmInvocations().getFirst();
    assertEquals("gpt-4", invocation.getInteraction().getLlm().getModel());
    assertEquals("complex-analysis", invocation.getInteraction().getId().getValue());
    assertTrue(invocation.getInteraction().getPromptContributors().size() >= 3);
}
```

---

## Integration Testing

Integration testing exercises complete agent workflows with real or mock external services while still avoiding actual LLM calls for predictability and speed.

Embabel integration testing is built on top of [Spring's integration testing support](https://docs.spring.io/spring-framework/reference/testing/integration.html), allowing you to work with real databases. Spring's integration with [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) is particularly useful.

### Using EmbabelMockitoIntegrationTest

Embabel provides `EmbabelMockitoIntegrationTest` as a base class that simplifies integration testing with convenient helper methods.

```java
class StoryWriterIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Test
    void shouldExecuteCompleteWorkflow() {
        var input = new UserInput("Write about artificial intelligence");

        var story = new Story("AI will transform our world...");
        var reviewedStory = new ReviewedStory(story, "Excellent.", Personas.REVIEWER);

        // Stub LLM calls with prompt matching
        whenCreateObject(contains("Craft a short story"), Story.class)
                .thenReturn(story);

        whenGenerateText(contains("You will be given a short story to review"))
                .thenReturn(reviewedStory.review());

        var invocation = AgentInvocation.create(agentPlatform, ReviewedStory.class);
        var result = invocation.invoke(input);

        assertNotNull(result);
        assertTrue(result.getContent().contains(story.text()));

        // Verify specific LLM interactions
        verifyCreateObjectMatching(
            prompt -> prompt.contains("Craft a short story"),
            Story.class,
            llm -> llm.getLlm().getTemperature() == 0.9 && llm.getToolGroups().isEmpty()
        );
        verifyGenerateTextMatching(prompt -> prompt.contains("You will be given a short story to review"));
        verifyNoMoreInteractions();
    }
}
```

```kotlin
class StoryWriterIntegrationTest : EmbabelMockitoIntegrationTest() {

    @Test
    fun `should execute complete workflow`() {
        val input = UserInput("Write about artificial intelligence")

        val story = Story("AI will transform our world...")
        val reviewedStory = ReviewedStory(story, "Excellent.", Personas.REVIEWER)

        whenCreateObject(contains("Craft a short story"), Story::class.java)
            .thenReturn(story)

        whenGenerateText(contains("You will be given a short story to review"))
            .thenReturn(reviewedStory.review)

        val invocation = AgentInvocation.create(agentPlatform, ReviewedStory::class.java)
        val result = invocation.invoke(input)

        assertNotNull(result)
        assertTrue(result.content.contains(story.text))

        verifyCreateObjectMatching(
            { prompt -> prompt.contains("Craft a short story") },
            Story::class.java
        ) { llm ->
            llm.llm.temperature == 0.9 && llm.toolGroups.isEmpty()
        }
        verifyGenerateTextMatching { prompt -> prompt.contains("You will be given a short story to review") }
        verifyNoMoreInteractions()
    }
}
```

### Key Integration Testing Features

| Feature | Description |
|---------|-------------|
| `whenCreateObject(prompt, outputClass)` | Mock object creation calls |
| `whenGenerateText(prompt)` | Mock text generation calls |
| `contains(...)` matching | Partial prompt matching in stubs |
| `verifyCreateObjectMatching()` | Verify prompts with custom matchers |
| `verifyGenerateTextMatching()` | Verify text generation calls |
| `verifyNoMoreInteractions()` | Ensure no unexpected LLM calls |

### LLM Configuration Testing in Integration Tests

Verify temperature settings, tool groups, and other LLM options:

```java
verifyCreateObjectMatching(
    prompt -> prompt.contains("Craft a short story"),
    Story.class,
    llm -> llm.getLlm().getTemperature() == 0.9
        && llm.getToolGroups().isEmpty()
);
```

---

## Best Practices

1. **Always set interaction IDs** via `.withId("interaction-id")` -- it makes assertions precise and readable.
2. **Verify both prompt content and LLM hyperparameters** (temperature, model) in every unit test.
3. **Use `FakeOperationContext.create()`** for unit tests; use `EmbabelMockitoIntegrationTest` for workflow tests under Spring Boot.
4. **Call `expectResponse()` in order** -- each call to `expectResponse()` is consumed by the next LLM invocation.
5. **Use `withExample()` / `withExamples()`** to test few-shot prompting behavior and verify examples appear as prompt contributors.
6. **For injected components** (e.g., `Ai` field), use Mockito or mockk directly rather than `FakePromptRunner`.
7. **For annotated agents** (`@Agent`, `@Action`), use `AgentMetadataReader` to extract metadata, then run via `AgentProcess` with `IntegrationTestUtils.dummyAgentPlatform()`.
8. **Access tools via `getInteraction().getTools()`** (actual tools from `withToolObject()`/`withTool()`), and tool groups via `getInteraction().getToolGroups()` (named requirements from `withToolGroup()`). Most tests should use `getTools()`.
9. **For integration tests with real LLM providers**, configure the model via `@SpringBootTest(properties = { "embabel.models.default-llm=..." })` and use `withDefaultLlm()` so the model is changeable without code changes.
10. **Building Gen AI applications is no different from building other software** -- test from the outset, not as an afterthought.
---

*Source: Embabel Agent v1.0.0 documentation*
