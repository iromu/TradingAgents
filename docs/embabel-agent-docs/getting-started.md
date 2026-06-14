Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 2.1. Quickstart
There are two GitHub template repos you can use to create your own project:
- Java template - github.com/embabel/java-agent-template
- Kotlin template - github.com/embabel/kotlin-agent-templateOr you can use our project creator to create a custom project:
| | The `uvx` command can be installed from the astral-uv package. It is a Python package and project manager used to run the Embabel project creator scripts. |
Now you have the code you need to run Embabel with LLMs from Open AI or Anthropic by using the included Maven profiles. Skip ahead to Environment Setup for API-key configuration, or detailed instructions on how to use other LLM providers.

### 2.2. Getting the Binaries
The easiest way to get started with Embabel Agent is to add the Spring Boot starter dependency to your project.
Embabel release binaries are published to Maven Central.
#### 2.2.1. Build Configuration
Add the appropriate Embabel Agent Spring Boot starter to your build file depending on your choice of application type:
##### Shell Starter
Starts the application in console mode with an interactive shell powered by Embabel.Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)**Features:**
- ✅ Interactive command-line interface
- ✅ Agent discovery and registration
- ✅ Human-in-the-loop capabilities
- ✅ Progress tracking and logging
- ✅ Development-friendly error handling
##### MCP Server Starter
Starts the application with HTTP listener where agents are autodiscovered and registered as MCP servers, available for integration via SSE, Streamable-HTTP or Stateless Streamable-HTTP protocols.Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)**Features:**
- ✅️ MCP protocol server implementation
- ✅️ Tool registration and discovery
- ✅️ JSON-RPC communication via SSE (Server-Sent Events), Streamable-HTTP or Stateless Streamable-HTTP
- ✅️ Integration with MCP-compatible clients
- ✅️ Security and sandboxing
##### Basic Agent Platform Starter
Initializes Embabel Agent Platform in the Spring Container.
Platform beans are available via Spring Dependency Injection mechanism.
Application startup mode (web, console, microservice, etc.) is determined by the Application Designer.Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)**Features:**
- ✅️ Application decides on startup mode (console, web application, etc)
- ✅️ Agent discovery and registration
- ✅️ Agent Platform beans available via Dependency Injection mechanism
- ✅️ Progress tracking and logging
- ✅️ Development-friendly error handling
##### Embabel Snapshots
If you want to use Embabel snapshots, you’ll need to add the Embabel repository to your build.Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
#### 2.2.2. Environment Setup
Before running your application, you’ll need to set up your environment with API keys for the LLM providers you plan to use.Example `.env` file:
```
OPENAI_API_KEY=your_openai_api_key_here
ANTHROPIC_API_KEY=your_anthropic_api_key_here
GEMINI_API_KEY=your_gemini_api_key_here
MISTRAL_API_KEY=your_mistral_api_key_here
```
If you added the binaries directly to your projecdt or want to use other LLM providers than Open AI and Anthropic you will also need to add some dependencies specific to those vendors. Just follow the instructions below for your vendor(s) of choice.
##### OpenAI Compatible (GPT-4, GPT-5, etc.)

- Required:
- `OPENAI_API_KEY`: API key for OpenAI or compatible services (e.g., Azure OpenAI, etc.)
- Optional:
- `OPENAI_BASE_URL`: base URL of the OpenAI deployment (for Azure AI use `{resource-name}.openai.azure.com/openai`)
- `OPENAI_COMPLETIONS_PATH`: custom path for completions endpoint (default: `/v1/completions`)
- `OPENAI_EMBEDDINGS_PATH`: custom path for embeddings endpoint (default: `/v1/embeddings`)Alternatively, configure via `application.yml`:
| **1** | API key with optional default for local development || **2** | Optional base URL override |
If you are not using the Embabel template projects you also need to add the `embabel-agent-starter-openai` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
##### OpenAi Custom

- Required:
- `OPENAI_CUSTOM_API_KEY`: API key for the OpenAI-compatible service
- Optional:
- `OPENAI_CUSTOM_BASE_URL`: base URL for the OpenAI-compatible API
- `OPENAI_CUSTOM_MODELS`: comma-separated list of custom model names to register (useful for OpenAI-compatible providers like Groq, Together AI, etc.)
- `OPENAI_CUSTOM_COMPLETIONS_PATH`: custom path for chat completions endpoint
- `OPENAI_CUSTOM_EMBEDDINGS_PATH`: custom path for embeddings endpointWhen using `OPENAI_CUSTOM_MODELS`, set `EMBABEL_MODELS_DEFAULT_LLM` to specify which model to use as the default.Example for using Groq:Alternatively, configure via `application.yml`:For APIs with non-standard paths (e.g., Z.AI), use the completions path override:You also need to add the `embabel-agent-starter-openai-custom` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
##### Anthropic (Claude 3.x, etc.)

- Required:
- `ANTHROPIC_API_KEY`: API key for Anthropic services
- Optional:
- `ANTHROPIC_BASE_URL`: base URL for Anthropic APIAlternatively, configure via `application.yml`:If you are not using the Embabel template projects you also need to add the `embabel-agent-starter-anthropic` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
##### DeepSeek

- Required:
- `DEEPSEEK_API_KEY`: API key for DeepSeek services
- Optional:
- `DEEPSEEK_BASE_URL`: base URL for DeepSeek API (default: `api.deepseek.com`)Alternatively, configure via `application.yml`:You also need to add the `embabel-agent-starter-deepseek` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
##### Google Gemini (OpenAI Compatible)
Uses the OpenAI-compatible endpoint for Gemini models.
- Required:
- `GEMINI_API_KEY`: API key for Google Gemini services
- Optional:
- `GEMINI_BASE_URL`: base URL for Gemini API (default: `generativelanguage.googleapis.com/v1beta/openai`)Alternatively, configure via `application.yml`:You also need to add the `embabel-agent-starter-gemini` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
##### Google GenAI (Native)
Uses the native Google GenAI SDK for direct access to Gemini models with full feature support including thinking mode.
- Required (API Key authentication):
- `GOOGLE_API_KEY`: API key for Google AI Studio
- Required (Vertex AI authentication - alternative to API key):
- `GOOGLE_PROJECT_ID`: Google Cloud project ID
- `GOOGLE_LOCATION`: Google Cloud region (e.g., `us-central1`)
| | Use API key authentication for Google AI Studio, or Vertex AI authentication for Google Cloud deployments.
Vertex AI authentication requires Application Default Credentials (ADC) to be configured. |

| | Gemini 3 models are only available in the `global` location on Vertex AI.
To use Gemini 3 with Vertex AI, you must set `GOOGLE_LOCATION=global`. |
To add Google GenAI support to your project add the `embabel-agent-starter-google-genai` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)Available LLM models include:
- `gemini-3.5-flash` - Frontier-level reasoning at Flash tier speed & cost
- `gemini-3.1-flash-lite` - Production lightweight and cost-effective model
- `gemini-3.1-pro-preview` - Latest Gemini 3.1 Pro preview with advanced reasoning
- `gemini-3.1-pro-preview-customtools` - Gemini 3.1 Pro optimized for custom tool usage
- `gemini-3.1-flash-lite-preview` - Lightweight and cost-effective latest generation model
- `gemini-2.5-pro` - High-performance model with thinking support
- `gemini-2.5-flash` - Best price-performance model
- `gemini-2.5-flash-lite` - Cost-effective high-throughput model
- `gemini-2.0-flash` - Fast and efficient
- `gemini-2.0-flash-lite` - Lightweight versionAvailable embedding models include:
- `gemini-embedding-001` - Recommended embedding model (3072 dimensions)Example configuration in `application.yml`:
| **1** | Set a Google GenAI model as the default LLM || **2** | Set a Google GenAI embedding model as the default embedding model || **3** | Google GenAI specific configuration || **4** | API key can be set here or via environment variable `GOOGLE_API_KEY` |

##### OCI Generative AI
To use OCI Generative AI with your agent, add the `embabel-agent-starter-oci-genai` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)The starter defaults to OCI config file authentication with `~/.oci/config` and profile `DEFAULT`.
At minimum, configure the compartment OCID and region.
When the standard OpenAI provider is not on the classpath, the OCI starter supplies OCI defaults for Embabel’s default LLM and embedding model, so an OCI-only application does not need to override `embabel.models.default-llm` just to start.Other supported authentication types are `INSTANCE_PRINCIPAL`, `RESOURCE_PRINCIPAL`, `WORKLOAD_IDENTITY`, `SESSION_TOKEN` and `SIMPLE`.
See the configuration reference for the full OCI property list.
##### Mistral AI

- Required:
- `MISTRAL_API_KEY`: API key for Mistral AI services
- Optional:
- `MISTRAL_BASE_URL`: base URL for Mistral AI API (default: `api.mistral.ai`)Alternatively, configure via `application.yml`:You also need to add the `embabel-agent-starter-mistral-ai` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)
##### LM Studio
LM Studio is a desktop application that lets you easily discover, download, and run powerful LLMs on your own computer (Windows, Mac, Linux) for free, enabling offline use, local document Q&A, and even hosting an OpenAI-compatible API server for your projects, making advanced AI accessible without relying on cloud services.
It supports formats like GGUF and offers privacy and control over your models.The LM Studio Local Server
allows you to run an LLM API server on localhost.To add LM Studio support, add the `embabel-agent-starter-lmstudio` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)Configure an LLM API on LM Studio by following these instructions.Specify the example configuration in `application.yml`.
Below I have an open-ai llm and embedding model downloaded on my LLM studio and exposed via the LLM Server.
##### Ollama
Ollama is an open source application that lets you easily discover, download, and run powerful LLMs on your own computer (Windows, Mac, Linux) for free, enabling offline use, local document Q&A, and even hosting an API server for your projects, making advanced AI accessible without relying on cloud services.The Ollama application allows you to run an LLM API server on localhost. Exposing both its own Ollama API and an Open-AI-compatible API.Get Ollama running locally by following these instructions.To use the Ollama API with your agent, add the `embabel-agent-starter-ollama` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)Specify the example configuration in `application.yml`. Embabel uses Spring AI configuration to configure the Ollama integration.To instead use the Open-AI-compatible API with your agent, add the `embabel-agent-starter-openai-custom` starter.Add this to your build system as follows:Maven (pom.xml)Gradle Kotlin DSL (build.gradle.kts)Gradle Groovy DSL (build.gradle)Specify the example configuration in `application.yml`. Embabel uses Spring AI configuration to configure the Ollama integration.

### 2.3. Getting Embabel Running

#### 2.3.1. Running the Examples
The quickest way to get started with Embabel is to run the examples:
| | Choose the `java` or `kotlin` scripts directory depending on your preference. |

#### 2.3.2. Prerequisites

- Java 21+
- API Key from OpenAI, Anthropic, or Google
- Maven 3.9+ (optional)Set your API keys:
| | For Google GenAI, you can use either `GOOGLE_API_KEY` (Google AI Studio) or Vertex AI authentication with `GOOGLE_PROJECT_ID` and `GOOGLE_LOCATION`. |

#### 2.3.3. Using the Shell
Spring Shell is an easy way to interact with the Embabel agent framework, especially during development.Type `help` to see available commands, or use `execute` / `x` to run an agent:
```
execute "Lynda is a Scorpio, find news for her" -p -r
```
The `-p` and `-r` flags log prompts and LLM responses respectively; omit them for quiet output.
Use `chat` for an interactive conversation, and `choose-goal` to inspect how Embabel ranks goals without running anything.For a full description of every command, flags, tab completion, and history shortcuts, see Embabel Shell.
#### 2.3.4. Example Commands
Try these commands in the shell:
```
# Simple horoscope agent
execute "My name is Sarah and I'm a Leo"

# Research with web tools (requires Docker Desktop with MCP extension)
execute "research the recent australian federal election. what is the position of the Greens party?"

# Fact checking
x "fact check the following: holden cars are still made in australia"
```

#### 2.3.5. Implementing Your Own Shell Commands
You can add custom shell commands to invoke specific agents directly during development.
See Implementing Custom Shell Commands for a full example and explanation.

### 2.4. Adding a Little AI to Your Application
Before we get into the magic of full-blown Embabel agents, let’s see how easy it is to add a little AI to your application using the Embabel framework.
Sometimes this is all you need.The simplest way to use Embabel is to inject an `OperationContext` and use its AI capabilities directly.
This approach is consistent with standard Spring dependency injection patterns.JavaKotlinThis example demonstrates several key aspects of Embabel’s design philosophy:
- **Standard Spring Integration**: The `Ai` object is injected like any other Spring dependency using constructor injection
- **Simple API**: Access AI capabilities through the `Ai` interface directly or `OperationContext.ai()`, which can also be injected in the same way
- **Flexible Configuration**: Configure LLM options like temperature on a per-call basis
- **Type Safety**: Generate structured objects directly with `createObject()` method
- **Consistent Patterns**: Works exactly like you’d expect any Spring component to workThe `Ai` type provides access to all of Embabel’s AI capabilities without requiring a full agent setup, making it perfect for adding AI features to existing applications incrementally.
| | The `Ai` and OperationContext` APIs are used throughout Embabel applications, as a convenient gateway to key AI and other functionality. |

### 2.5. Writing Your First Agent
The easiest way to create your first agent is to use the Java or Kotlin template repositories.
#### 2.5.1. Example: WriteAndReviewAgent
The template includes a `WriteAndReviewAgent` that demonstrates key concepts:JavaKotlin
#### 2.5.2. Key Concepts Demonstrated
**Multiple LLMs with Different Configurations:**
- Writer LLM uses high temperature (0.8) for creativity
- Reviewer LLM uses low temperature (0.2) for analytical review
- Different personas guide the model behavior**Actions and Goals:**
- `@Action` methods are the steps the agent can take
- `@AchievesGoal` marks the final action that completes the agent’s work**Domain Objects:**
- `Story` and `ReviewedStory` are strongly-typed domain objects
- Help structure the interaction between actions
#### 2.5.3. Running Your Agent
Set your API keys and run the shell:In the shell, try:
```
x "Tell me a story about a robot learning to paint"
```
The agent will:
1.Generate a creative story using the writer LLM
1.Review and improve it using the reviewer LLM
1.Return the final reviewed story
#### 2.5.4. Next Steps

- Explore the examples repository for more complex agents
- Read the Reference Documentation for detailed API information
- Try building your own domain-specific agents
## 3. Embabel Shell
The easiest way to get started with Embabel is via Spring Shell.