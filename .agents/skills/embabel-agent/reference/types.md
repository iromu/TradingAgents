# Core Types

## LlmOptions

The `LlmOptions` class specifies which LLM to use and its hyperparameters. It's defined in `embabel-common` and provides a fluent API for LLM configuration.

### Key Methods

| Method | Description |
|--------|-------------|
| `withModel(String)` | Specify the model name |
| `withRole(String)` | Specify the model role (must be defined in `embabel.models.llms.<role>`) |
| `withTemperature(Double)` | Set creativity / randomness (`0.0`–`1.0`) |
| `withTopP(Double)` | Set nucleus sampling parameter |
| `withTopK(Integer)` | Set top-K sampling parameter |
| `withPersona(String)` | Add a system message persona |

`LlmOptions` is deserializable, so you can set properties in `application.yml` — a powerful way of externalizing not only models but hyperparameters.

### Factory Methods

| Factory | Purpose |
|---------|---------|
| `LlmOptions.withDefaultLlm()` | Resolve the configured `default-llm` |
| `LlmOptions.withModel(String)` | Hard-code a model name |
| `LlmOptions.withRole(String)` | Resolve via `embabel.models.llms.<role>` |
| `LlmOptions.fromCriteria(ModelSelectionCriteria)` | Pick model by capability criteria |

---

## PromptRunner

All LLM calls in user applications should be made via the `PromptRunner` interface. Once created, a `PromptRunner` can run multiple prompts with the same LLM, hyperparameters, tool groups, and `PromptContributors`.

### Getting a PromptRunner

```java
@Action
public Story createStory(UserInput input, OperationContext context) {
    var runner = context.ai().withDefaultLlm();
    var customRunner = context.ai().withLlm(
        LlmOptions.withModel(OpenAiModels.GPT_4O_MINI).withTemperature(0.8));
    return customRunner.createObject("Write a story about: " + input.getContent(), Story.class);
}
```

### PromptRunner Methods

#### Core Object Creation

| Method | Description |
|--------|-------------|
| `createObject(String, Class<T>)` | Create a typed object from a prompt; throws on persistent failure (with retry) |
| `createObjectIfPossible(String, Class<T>)` | Try to create an object; returns `null` on failure |
| `generateText(String)` | Generate simple text response |

> **Tip:** Normally you want to use one of the `createObject` methods to ensure the response is typed correctly.

#### Tool and Context Management

| Method | Description |
|--------|-------------|
| `withToolGroup(String)` | Add tool groups for LLM access |
| `withToolObject(Object)` | Add domain objects with `@Tool` methods |
| `withPromptContributor(PromptContributor)` | Add context contributors |
| `withImage(AgentImage)` | Add an image to the prompt (vision-capable LLMs) |
| `withImages(AgentImage...)` | Add multiple images to the prompt |
| `withDocument(AgentDocument)` | Attach a document (PDF, Office, etc.) |
| `withMessage(Message)` | Attach a raw Embabel `Message` |
| `withTool(Subagent.ofClass(...))` | Enable handoff to another agent |
| `withToolCallContext(Map)` | Add per-interaction tool call context |

#### LLM Configuration

| Method | Description |
|--------|-------------|
| `withLlm(LlmOptions)` | Use specific LLM configuration |
| `withGenerateExamples(Boolean)` | Control example generation |
| `withThinking(Thinking)` | Enable native reasoning mode |
| `withStreaming(Boolean)` | Enable streaming responses |
| `withToolNotFoundPolicy(ToolNotFoundPolicy)` | Control tool-name recovery strategy |

#### Returning a Specific Type (Creating API)

| Method | Description |
|--------|-------------|
| `creating(Class<T>)` | Enter the `Creating` fluent API for returning a particular type |
| `withExample(String prompt, T example)` | Add a few-shot example (rendered as JSON) |
| `fromPrompt(String)` | Execute the call with the given prompt |

#### Advanced

| Method | Description |
|--------|-------------|
| `rendering(String)` | Use Jinja templates for prompts (returns `Rendering` interface) |
| `evaluateCondition(String, String)` | Evaluate a boolean condition |

---

## Validation (JSR-380)

Embabel supports [JSR-380](https://beanvalidation.org/2.0/) bean validation annotations on domain objects. When creating objects via `PromptRunner.createObject` or `createObjectIfPossible`, validation is automatically performed after deserialization.

If validation fails, Embabel transparently retries the LLM call, describing the validation errors to the LLM so it can correct its response.

If validation fails a second time, `InvalidLlmReturnTypeException` is thrown — this will trigger replanning if not caught.

### Example

```java
public class User {
    @NotNull(message = "Name cannot be null")
    private String name;

    @Size(min = 10, max = 200, message = "About Me must be between 10 and 200 characters")
    private String aboutMe;

    @Min(value = 18, message = "Age should not be less than 18")
    @Max(value = 150, message = "Age should not be greater than 150")
    private int age;

    @Email(message = "Email should be valid")
    private String email;
}
```

### Custom Validators

You can use custom annotations with validators that are injected by Spring:

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PalindromeValidator.class)
public @interface MustBePalindrome {
    String message() default "Must be a palindrome";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class PalindromeValidator implements ConstraintValidator<MustBePalindrome, String> {
    private final Ai ai;  // Spring injects dependencies

    @Override
    public boolean isValid(String field, ConstraintValidatorContext context) {
        if (field == null) return false;
        return field.equals(new StringBuilder(field).reverse().toString());
    }
}
```

---

## AgentImage

Represents an image for use with vision-capable LLMs.

### Factory Methods

| Method | Description |
|--------|-------------|
| `AgentImage.fromFile(File)` | Load from file (auto-detects MIME type from common extensions) |
| `AgentImage.fromPath(Path)` | Load from path (auto-detects MIME type) |
| `AgentImage.create(String, byte[])` | Create with explicit MIME type and byte array |
| `AgentImage.fromBytes(String, byte[])` | Create from filename and bytes (auto-detects MIME type) |

If auto-detection fails for a supported image format, use `AgentImage.create()` with an explicit MIME type.

### Usage

```java
var image = AgentImage.fromFile(imageFile);

var answer = context.ai()
    .withLlm(AnthropicModels.CLAUDE_35_HAIKU)  // Vision-capable model required
    .withImage(image)
    .generateText("What is in this image?");
```

---

## AgentDocument

Represents a document for use with document-capable LLMs.

### Factory Methods

| Method | Description |
|--------|-------------|
| `AgentDocument.fromFile(File)` | Load from file (auto-detects MIME type) |
| `AgentDocument.fromPath(Path)` | Load from path (auto-detects MIME type) |
| `AgentDocument.create(String, byte[], String?)` | Create with explicit MIME type, byte array, and optional filename |
| `AgentDocument.fromBytes(String, byte[])` | Create from filename and bytes (auto-detects MIME type) |

Embabel can identify PDF, CSV, Microsoft Office documents (`.doc`, `.docx`, `.xlsx`), and OpenDocument files (`.odt`, `.ods`, `.odp`) by MIME type. Provider support varies by model and adapter — verify that the selected LLM can consume the document format you send.

### Usage

```java
var documentPath = java.nio.file.Paths.get("reports/quarterly-report.pdf");
var document = AgentDocument.fromPath(documentPath);

var answer = context.ai()
    .withDefaultLlm()  // Document-capable model required
    .withDocument(document)
    .generateText("Summarize this document");
```

> **Note:** The Gemini autoconfigure provider uses Google's OpenAI-compatible endpoint, which supports text and image inputs but not document file parts. Use the native Google GenAI provider for Gemini document input.

---

*Source: Embabel Agent v1.0.0 documentation — `reference/types`*
