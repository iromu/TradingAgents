Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.8. Core Types

#### 4.8.1. LlmOptions
The `LlmOptions` class specifies which LLM to use and its hyperparameters.
It’s defined in the embabel-common project and provides a fluent API for LLM configuration:JavaKotlin**Important Methods:**
- `withModel(String)`: Specify the model name
- `withRole(String)`: Specify the model role. The role must be one defined in configuration via `embabel.models.llms.<role>=<model-name>`
- `withTemperature(Double)`: Set creativity/randomness (0.0-1.0)
- `withTopP(Double)`: Set nucleus sampling parameter
- `withTopK(Integer)`: Set top-K sampling parameter
- `withPersona(String)`: Add a system message persona`LlmOptions` is deserializable, so you can set properties of type `LlmOptions` in `application.yml` and other application configuration files.
This is a powerful way of externalizing not only models, but hyperparameters.
#### 4.8.2. PromptRunner
All LLM calls in user applications should be made via the `PromptRunner` interface.
Once created, a `PromptRunner` can run multiple prompts with the same LLM, hyperparameters, tool groups and `PromptContributors`.
##### Getting a PromptRunner
You obtain a `PromptRunner` from an `OperationContext` using the fluent API:JavaKotlin
##### PromptRunner Methods
**Core Object Creation:**
- `createObject(String, Class<T>)`: Create a typed object from a prompt, otherwise throw an exception. An exception triggers retry. If retry fails repeatedly, re-planning occurs.
- `createObjectIfPossible(String, Class<T>)`: Try to create an object, return null on failure.
This can cause replanning.
- `generateText(String)`: Generate simple text response
| | Normally you want to use one of the `createObject` methods to ensure the response is typed correctly. |
**Tool and Context Management:**
- `withToolGroup(String)`: Add tool groups for LLM access
- `withToolObject(Object)`: Add domain objects with @Tool methods
- `withPromptContributor(PromptContributor)`: Add context contributors
- `withImage(AgentImage)`: Add an image to the prompt for vision-capable LLMs
- `withImages(AgentImage…​)`: Add multiple images to the prompt**LLM Configuration:**
- `withLlm(LlmOptions)`: Use specific LLM configuration
- `withGenerateExamples(Boolean)`: Control example generation**Returning a Specific Type**
- `creating(Class<T>)`: Go into the `Creating` fluent API for returning a particular type.For example:JavaKotlinThe main reason to do this is to add strongly typed examples for few-shot prompting.
For example:JavaKotlin
| **1** | **Example**: The example will be included in the prompt in JSON format to guide the LLM. |
**Working with Images:**JavaKotlin
| **1** | **Vision-capable model required**: Use Claude 3.x, GPT-4 Vision, or other multimodal LLMs || **2** | **Add image**: Images are sent with the text prompt to the LLM. Can be used multiple times for multiple images. |
**Advanced Features:**
- `rendering(String)`: Use Jinja templates for prompts (returns `Rendering` interface)
- `withTool(Subagent.ofClass(MyAgent.class).consuming(MyInput.class))`: Enable handoffs to other agents (see Subagent: Agent Handoffs as Tools)
- `evaluateCondition(String, String)`: Evaluate boolean condition**Validation**Embabel supports JSR-380 bean validation annotations on domain objects.
When creating objects via `PromptRunner.createObject` or `createObjectIfPossible`, validation is automatically performed after deserialization.
If validation fails, Embabel transparently retries the LLM call to obtain a valid object,
describing the validation errors to the LLM to help it correct its response.If validation fails a second time, `InvalidLlmReturnTypeException` is thrown.
This will trigger replanning if not caught.
You can also choose to catch it within the action method making the LLM call,
and take appropriate action in your own code.Simple example of annotation use:JavaKotlinYou can also use custom annotations with validators that will be injected by Spring. For example:JavaKotlin
| **1** | Define the custom annotation || **2** | Apply the annotation to a field || **3** | Implement the validator as a Spring component. Note the `@Component` annotation. || **4** | Spring will inject the validator with dependencies, such as the `Ai` instance in this case |
Thus we have standard JSR-280 validation with full Spring dependency injection support.
#### 4.8.3. AgentImage
Represents an image for use with vision-capable LLMs.**Factory Methods:**
- `AgentImage.fromFile(File)`: Load from file (auto-detects MIME type from common extensions)
- `AgentImage.fromPath(Path)`: Load from path (auto-detects MIME type)
- `AgentImage.create(String, byte[])`: Create with explicit MIME type and byte array
- `AgentImage.fromBytes(String, byte[])`: Create from filename and bytes (auto-detects MIME type)For uncommon image formats or if auto-detection fails, use `AgentImage.create()` with an explicit MIME type.