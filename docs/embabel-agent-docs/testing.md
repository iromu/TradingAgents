Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.37. Testing
Like Spring, Embabel facilitates testing of user applications.
The framework provides comprehensive testing support for both unit and integration testing scenarios.
| | Building Gen AI applications is no different from building other software.
Testing is critical to delivering quality software and must be considered from the outset. |

#### 4.37.1. Unit Testing
Unit testing in Embabel enables testing individual agent actions without involving real LLM calls.Embabel’s design means that agents are usually POJOs that can be instantiated with fake or mock objects.
Actions are methods that can be called directly with test fixtures.
In additional to your domain objects, you will pass a text fixture for the Embabel `OperationContext`, enabling you to intercept and verify LLM calls.The framework provides `FakePromptRunner` and `FakeOperationContext` to mock LLM interactions while allowing you to verify prompts, hyperparameters, and business logic.
Alternatively you can use mock objects.
Mockito is the default choice for Java; mockk for Kotlin.
##### Testing Prompts and Hyperparameters
Here are unit tests from the Java Agent Template and Kotlin Agent Template repositories, using Embabel fake objects:JavaKotlin
##### Testing the Fluent API: withId() and creating()
The `FakePromptRunner` fully supports the fluent API patterns used in production code, enabling comprehensive unit testing of agents that use `withId()` for interaction tracing and `creating()` for structured object creation with examples.**Testing withId() for Interaction Tracing:**The `withId()` method sets an interaction ID for better log tracing. In tests, you can verify the interaction ID was correctly set:JavaKotlin**Testing creating() with withExample():**The `creating()` API allows you to provide strongly-typed examples to improve LLM output quality. In tests, you can verify examples were included:JavaKotlin**Using CreationExample for Reusable Examples:**For cleaner code and reusability, you can use the `CreationExample` data class to define examples that can be shared across tests or passed as collections:JavaKotlin**Adding Multiple Examples with withExamples():**When you have many examples to add, use `withExamples()` to pass them as a list or vararg. This is especially useful when examples are loaded from a file or database:JavaKotlinYou can also use vararg syntax for inline example lists:JavaKotlin**Full Fluent API Chain Example:**Here’s a complete example showing how to test an action that uses all the fluent API features:JavaKotlin
##### Key Testing Patterns Demonstrated
**Testing Prompt Content:**
- Use `context.getLlmInvocations().getFirst().getPrompt()` to get the actual prompt sent to the LLM
- Verify that key domain data is properly included in the prompt using `assertTrue(prompt.contains(…​))`**Testing Tools:**
- Access tools via `getInteraction().getTools()` to verify tools added via `withToolObject()` or `withTool()`
- Access tool group requirements via `getInteraction().getToolGroups()` to verify named tool group requirements added via `withToolGroup(ToolGroupRequirement)`
| | `getTools()` returns actual `Tool` instances (from `withToolObject()` and `withTool()`), while `getToolGroups()` returns `ToolGroupRequirement` objects (named requirements like "web_search" added via `withToolGroup()`). Most tests should use `getTools()`. |
**Testing with Spring Dependencies:**
- Mock Spring-injected services like `HoroscopeService` using standard mocking frameworks - Pass mocked dependencies to agent constructor for isolated unit testing
##### Testing Multiple LLM Interactions
JavaKotlinYou can also use Mockito or mockk directly.
Consider this component, using direct injection of `Ai`:JavaKotlinA unit test using Mockito (Java) or mockk (Kotlin) to verify prompt and hyperparameters:JavaKotlin
#### 4.37.2. Integration Testing
Integration testing exercises complete agent workflows with real or mock external services while still avoiding actual LLM calls for predictability and speed.This can ensure:
- Agents are picked up by the agent platform
- Data flow is correct within agents
- Failure scenarios are handled gracefully
- Agents interact correctly with each other and external systems
- The overall workflow behaves as expected
- LLM prompts and hyperparameters are correctly configuredEmbabel integration testing is built on top of Spring’s excellent integration testing support, thus allowing you to work with real databases if you wish.
Spring’s integration with Testcontainers is particularly userul.
##### Using EmbabelMockitoIntegrationTest
Embabel provides `EmbabelMockitoIntegrationTest` as a base class that simplifies integration testing with convenient helper methods:JavaKotlin
##### Key Integration Testing Features
**Base Class Benefits:**
- `EmbabelMockitoIntegrationTest` handles Spring Boot setup and LLM mocking automatically
- Provides `agentPlatform` and `llmOperations` pre-configured
- Includes helper methods for common testing patterns**Convenient Stubbing Methods:**
- `whenCreateObject(prompt, outputClass)`: Mock object creation calls
- `whenGenerateText(prompt)`: Mock text generation calls
- Support for both exact prompts and `contains()` matching
- Supports streaming calls by calling `supportsStreaming(true)` in test setup.**Advanced Verification:**
- `verifyCreateObjectMatching()`: Verify prompts with custom matchers
- `verifyGenerateTextMatching()`: Verify text generation calls
- `verifyNoMoreInteractions()`: Ensure no unexpected LLM calls**LLM Configuration Testing:**
- Verify temperature settings: `llm.getLlm().getTemperature() == 0.9`
- Check tool groups: `llm.getToolGroups().isEmpty()`
- Validate persona and other LLM options
##### Integration Tests with LLM
Embabel provides integration tests that exercise complete workflows with real LLM providers to verify end-to-end functionality.
These tests are located in the autoconfiguration modules for each LLM provider and verify that the integration with the provider’s API works correctly, including features like guardrails, thinking responses, and structured output.**Examples of LLM Integration Tests:**
| Test Name | Description | LLM Provider || LLMOpenAiGuardRailsIntegrationIT | Tests OpenAI integration with guardrails, including moderation API integration using both OpenAI SDK and Spring AI, and validates guardrail invocation for structured output | OpenAI (gpt-4.1-mini) || LLMAnthropicGuardRailsIntegrationIT | Tests Anthropic integration with guardrails, validating that AssistantMessageGuardRail is correctly invoked for structured object responses | Anthropic (claude-sonnet-4-5) || LLMGeminiGuardRailsIntegrationIT | Tests Gemini integration with guardrails, verifying guardrail functionality with structured output for Google’s Gemini models | Gemini (gemini-2.5-flash) |

###### Test Structure
These integration tests follow a consistent structure designed to test real LLM interactions while maintaining control over the test environment.**1. Test Location:**Integration tests are located within autoconfiguration modules rather than in separate test modules.
This placement ensures tests have direct access to the autoconfiguration classes they’re testing and can verify that Spring Boot correctly initializes all required beans.**2. Environment Properties:**Each test configures the Spring environment through the `@SpringBootTest` annotation’s `properties` attribute.
These properties configure the LLM models, timeouts, retries, and logging levels for the test environment.
| **1** | The `default-llm` property configures which LLM model to use by default when `ai.withDefaultLlm()` is called |
**3. Active Profiles:**The `@ActiveProfiles` annotation activates Spring profiles that enable specific functionality for the test environment.
The "thinking" profile enables extended thinking capabilities and additional logging for LLM operations.
| **1** | Activates the "thinking" profile which enables enhanced tracing and thinking block extraction |
**4. Configuration Properties Scanning:**The `@ConfigurationPropertiesScan` annotation enables Spring Boot to discover and bind configuration properties classes.
This is essential for integration tests to load all configuration properties from the `embabel.agent` and `embabel.example` packages.**5. Component Scanning:**The `@ComponentScan` annotation configures component scanning to discover Spring beans.
Integration tests typically exclude certain components (like exception handlers) that might interfere with test execution.**6. Importing Autoconfiguration:**The `@Import` annotation explicitly imports the autoconfiguration class being tested.
This ensures the specific LLM provider’s autoconfiguration is loaded and all required beans are created.
| **1** | Imports both the OpenAI autoconfiguration and test-specific guardrail configuration |
**7. Autowired Dependencies:**Integration tests autowire the beans needed for testing.
While tests may autowire multiple components for verification purposes, **only the `Ai` interface is required** to execute LLM operations.
| **1** | The `Ai` interface is the primary entry point and the only required dependency for making LLM calls || **2** | Additional autowired components used for verification and testing purposes |

###### Example Test: OpenAI GuardRails Integration
The following example demonstrates a complete integration test from `LLMOpenAiGuardRailsIntegrationIT.java`:
| **1** | Logging statement to track test execution || **2** | Thread-safe list to track guardrail invocations || **3** | Custom guardrail implementation that tracks when it’s called || **4** | Configure the prompt runner using the default LLM (configured as gpt-4.1-mini in test properties) || **5** | Execute the LLM call to create a structured object || **6** | Verify the result and guardrail invocation |

| | The test uses `withDefaultLlm()` which references the model configured in the `@SpringBootTest` properties (`embabel.models.default-llm=gpt-4.1-mini`).
This approach makes tests more flexible and allows the model to be changed without modifying test code. |

##### Testing Annotated Agents
When testing agents built with `@Agent` and `@Action` annotations, you need to verify that:
- The agent metadata is correctly constructed from annotations
- Actions execute with the correct behavior (retry policies, preconditions, etc.)
- The planner selects and executes actions appropriately
- Domain-specific logic works as expected**Test Structure for Annotated Agents:**The key steps to test an annotated agent are:
1.Create an instance of your annotated agent class
1.Use `AgentMetadataReader` to extract agent metadata from annotations
1.Create an `AgentProcess` with a dummy or real `AgentPlatform`
1.Provide input data and run the process
1.Verify the output and any side effects**Example: Testing Action Retry Policy**Here’s a concise example from `RetryActionAnnotationJavaTest` showing how to test an agent with retry behavior:
| **1** | Instantiate the annotated agent with any required dependencies || **2** | Use `AgentMetadataReader` to parse annotations and create agent metadata || **3** | Configure the planner type (UTILITY, GOAP, etc.) for the test || **4** | Provide input data as a map - keys match action parameter names || **5** | Run the process and extract the result of the expected type || **6** | Verify behavior using instance fields (like invocation counters) |
**The Annotated Agent Under Test:**
| **1** | Use instance fields to track invocations and verify behavior || **2** | Configure action behavior through annotations (retry policy, preconditions, etc.) || **3** | First invocation fails to test retry behavior || **4** | Second invocation succeeds |
**Key Testing Patterns:**
- **Dummy AgentPlatform**: Use `IntegrationTestUtils.dummyAgentPlatform()` for lightweight testing without Spring context
- **Instance State**: Access instance fields directly to verify internal behavior (invocation counts, state changes)
- **Input Map**: Provide action parameters as a `Map<String, Object>` where keys match parameter names
- **Result Extraction**: Use `agentProcess.run().resultOfType(ExpectedType.class)` to get strongly-typed results
- **Exception Testing**: Use `assertThrows()` to verify failure scenarios and retry exhaustion