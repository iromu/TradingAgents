Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.5. Configuration

#### 4.5.1. Enabling Embabel
Annotate your Spring Boot application class to get agentic behavior.Example:JavaKotlinThis is a normal Spring Boot application class.
You can add other Spring Boot annotations as needed.You also need to add the dependency and configuration for your LLM provider(s) of choice.
#### 4.5.2. Configuration Properties
The following table lists all available configuration properties in Embabel Agent Platform.
Properties are organized by their configuration prefix and include default values where applicable.
They can be set via `application.properties`, `application.yml`, profile-specific configuration files or environment variables, as per standard Spring configuration practices.
##### Setting default LLM and roles
From `ConfigurableModelProviderProperties` - configuration for default LLMs and role-based model selection.
| Property | Type | Default | Description || `embabel.models.default-llm` | String | `gpt-4.1-mini` | Default LLM name. It’s good practice to override this in configuration. || `embabel.models.default-embedding-model` | String | `null` | Default embedding model name. Need not be set, in which case it defaults to null. || `embabel.models.llms` | Map<String, String> | `{}` | Map of role to LLM name. Each entry will require an LLM to be registered with the same name. May not include the default LLM. || `embabel.models.embedding-services` | Map<String, String> | `{}` | Map of role to embedding service name. Does not need to include the default embedding service. You can create as many roles as you wish. |
Role-based model selection allows you to assign specific LLMs or embedding services to different roles within your application.
For example:It’s good practice to decouple your code from specific models in this way.
##### Platform Configuration
From `AgentPlatformProperties` - unified configuration for all agent platform properties.
| Property | Type | Default | Description || `embabel.agent.platform.name` | String | `embabel-default` | Core platform identity name || `embabel.agent.platform.description` | String | `Embabel Default Agent Platform` | Platform description |

##### Logging Personality
Configuration for agent logging output style and theming.
| Property | Type | Default | Description || `embabel.agent.logging.personality` | String | *(none)* | Themed logging messages to add personality to agent output |

| Value | Description || `starwars` | Star Wars themed logging messages || `severance` | Severance themed logging messages. Praise Kier || `colossus` | Colossus: The Forbin Project themed messages || `hitchhiker` | Hitchhiker’s Guide to the Galaxy themed messages || `montypython` | Monty Python themed logging messages |
Example Configuration
##### Agent Scanning
From `AgentPlatformProperties.ScanningConfig` - configures scanning of the classpath for agents.
| Property | Type | Default | Description || `embabel.agent.platform.scanning.annotation` | Boolean | `true` | Whether to auto register beans with @Agent and @Agentic annotation || `embabel.agent.platform.scanning.bean` | Boolean | `false` | Whether to auto register as agents Spring beans of type `Agent` |

##### Ranking Configuration
From `AgentPlatformProperties.RankingConfig` - configures ranking of agents and goals based on user input when the platform should choose the agent or goal.
| Property | Type | Default | Description || `embabel.agent.platform.ranking.llm` | String | `null` | Name of the LLM to use for ranking, or null to use auto selection || `embabel.agent.platform.ranking.max-attempts` | Int | `5` | Maximum number of attempts to retry ranking || `embabel.agent.platform.ranking.backoff-millis` | Long | `100` | Initial backoff time in milliseconds || `embabel.agent.platform.ranking.backoff-multiplier` | Double | `5.0` | Multiplier for backoff time || `embabel.agent.platform.ranking.backoff-max-interval` | Long | `180000` | Maximum backoff time in milliseconds |

##### LLM Operations
From `AgentPlatformProperties.LlmOperationsConfig` - configuration for LLM operations including prompts and data binding.
| Property | Type | Default | Description || `embabel.agent.platform.llm-operations.prompts.maybe-prompt-template` | String | `maybe_prompt_contribution` | Template for "maybe" prompt, enabling failure result when LLM lacks information || `embabel.agent.platform.llm-operations.prompts.generate-examples-by-default` | Boolean | `true` | Whether to generate examples by default || `embabel.agent.platform.llm-operations.data-binding.max-attempts` | Int | `10` | Maximum retry attempts for data binding || `embabel.agent.platform.llm-operations.data-binding.fixed-backoff-millis` | Long | `30` | Fixed backoff time in milliseconds between retries |

##### Tool Loop
From `ToolLoopConfiguration` - configuration for tool loop execution.
| Property | Type | Default | Description || `embabel.agent.platform.toolloop.type` | String | `default` | Tool loop type: `default` (sequential) or `parallel` (experimental) || `embabel.agent.platform.toolloop.max-iterations` | Int | `20` | Maximum number of tool loop iterations || `embabel.agent.platform.toolloop.parallel.per-tool-timeout` | Duration | `30s` | Timeout for individual tool execution in parallel mode || `embabel.agent.platform.toolloop.parallel.batch-timeout` | Duration | `60s` | Timeout for entire batch of parallel tools || `embabel.agent.platform.toolloop.empty-response.max-retries` | Int | `0` | Maximum consecutive empty-response retries before throwing `EmptyLlmResponseException`. `0` (default) preserves existing behaviour — the loop exits with blank content. Any value `> 0` activates `RetryWithFeedbackPolicy`. || `embabel.agent.platform.toolloop.empty-response.nudge-message` | String | *(see below)* | Message appended to the conversation as a synthetic `UserMessage` when the LLM goes silent. Only used when `max-retries > 0`. Default nudges the model to take one concrete action. |

###### Empty-Response Handling
Weak open-weights chat models (such as `gpt-oss-20b` or some Qwen variants) occasionally return blank text with no further tool calls after a tool result, when the model has run out of ideas about what to do next.
Without intervention the tool loop exits with empty content, which the rendering layer surfaces as `EmptyLlmResponseException`.The `empty-response` configuration controls whether the loop gives the model a second chance.
Setting `max-retries: 1` activates `RetryWithFeedbackPolicy`: when an empty response is detected the loop appends the configured nudge message as a synthetic `UserMessage` and re-invokes the LLM in the same loop iteration.
The retry counter is reset on any non-empty response, so retries are bounded per consecutive failure rather than cumulative across the whole loop.Example for a deployment running a smaller chat model:For most deployments using strong frontier models the default (`max-retries: 0`) is correct — empty responses are rare, and the typed exception lets callers handle the case explicitly.
This setting is provided to make small / local model deployments more robust without forcing every caller to wrap LLM invocations in their own retry logic.For programmatic configuration (custom retry counts or messages), inject your own `EmptyResponsePolicy` bean — the auto-configuration honours `@ConditionalOnMissingBean`.
##### Process ID Generation
From `AgentPlatformProperties.ProcessIdGenerationConfig` - configuration for process ID generation.
| Property | Type | Default | Description || `embabel.agent.platform.process-id-generation.include-version` | Boolean | `false` | Whether to include version in process ID generation || `embabel.agent.platform.process-id-generation.include-agent-name` | Boolean | `false` | Whether to include agent name in process ID generation |

##### Autonomy Configuration
From `AgentPlatformProperties.AutonomyConfig` - configures thresholds for agent and goal selection.
Certainty below thresholds will result in failure to choose an agent or goal.
| Property | Type | Default | Description || `embabel.agent.platform.autonomy.agent-confidence-cut-off` | Double | `0.6` | Confidence threshold for agent operations || `embabel.agent.platform.autonomy.goal-confidence-cut-off` | Double | `0.6` | Confidence threshold for goal achievement |

##### Model Provider Configuration
From `AgentPlatformProperties.ModelsConfig` - model provider integration configurations.
###### Anthropic

| Property | Type | Default | Description || `embabel.agent.platform.models.anthropic.max-attempts` | Int | `10` | Maximum retry attempts || `embabel.agent.platform.models.anthropic.backoff-millis` | Long | `5000` | Initial backoff time in milliseconds || `embabel.agent.platform.models.anthropic.backoff-multiplier` | Double | `5.0` | Backoff multiplier || `embabel.agent.platform.models.anthropic.backoff-max-interval` | Long | `180000` | Maximum backoff interval in milliseconds |

###### OpenAI

| Property | Type | Default | Description || `embabel.agent.platform.models.openai.max-attempts` | Int | `10` | Maximum retry attempts || `embabel.agent.platform.models.openai.backoff-millis` | Long | `5000` | Initial backoff time in milliseconds || `embabel.agent.platform.models.openai.backoff-multiplier` | Double | `5.0` | Backoff multiplier || `embabel.agent.platform.models.openai.backoff-max-interval` | Long | `180000` | Maximum backoff interval in milliseconds |

###### Google GenAI (Native)
Uses the native Google GenAI SDK (`spring-ai-google-genai`) for direct access to Gemini models with full feature support.
| Property | Type | Default | Description || `embabel.agent.platform.models.googlegenai.max-attempts` | Int | `10` | Maximum retry attempts || `embabel.agent.platform.models.googlegenai.backoff-millis` | Long | `5000` | Initial backoff time in milliseconds || `embabel.agent.platform.models.googlegenai.backoff-multiplier` | Double | `5.0` | Backoff multiplier || `embabel.agent.platform.models.googlegenai.backoff-max-interval` | Long | `180000` | Maximum backoff interval in milliseconds |
Google GenAI models (both LLM and embedding) are configured via the `embabel-agent-starter-google-genai` starter dependency.The following embedding models are available:
| Model Name | Model ID | Dimensions | Price (per 1M tokens) || `gemini_embedding_001` | `gemini-embedding-001` | 3072 | $0.15 |
The following environment variables control authentication:
| Environment Variable | Description || `GOOGLE_API_KEY` | API key for Google AI Studio authentication || `GOOGLE_PROJECT_ID` | Google Cloud project ID (for Vertex AI authentication) || `GOOGLE_LOCATION` | Google Cloud region, e.g., `us-central1` (for Vertex AI authentication) |

| | Either `GOOGLE_API_KEY` or both `GOOGLE_PROJECT_ID` and `GOOGLE_LOCATION` must be set. |

| | Gemini 3 models are only available in the `global` location on Vertex AI.
To use Gemini 3 with Vertex AI, you must set `GOOGLE_LOCATION=global`. |
To add new Google GenAI embedding models, edit the configuration file:
```
embabel-agent-autoconfigure/models/embabel-agent-google-genai-autoconfigure/
 src/main/resources/models/google-genai-models.yml
```

###### OCI Generative AI
OCI Generative AI models are configured through the `embabel-agent-starter-oci-genai` starter dependency.
The starter registers OCI chat and embedding models from bundled metadata and uses the OCI Java SDK authentication providers.When the standard OpenAI provider is not on the classpath, the OCI starter supplies OCI defaults for Embabel’s default
LLM and embedding model:Override those values in application configuration if you want another OCI model.
Use OCI model ids for Embabel model selection.
The starter’s Spring bean names are Java-friendly aliases, for example `cohere_command_a` for `cohere.command-a-03-2025`
and `llama_33_70b` for `meta.llama-3.3-70b-instruct`.
| Property | Type | Default | Description || `embabel.agent.platform.models.ocigenai.authentication-type` | Enum | `FILE` | Authentication provider to use. Supported values are `FILE`, `INSTANCE_PRINCIPAL`, `RESOURCE_PRINCIPAL`, `WORKLOAD_IDENTITY`, `SESSION_TOKEN` and `SIMPLE`. || `embabel.agent.platform.models.ocigenai.config-file` | String | `~/.oci/config` | OCI config file path for `FILE` authentication, or optional config file path for `SESSION_TOKEN` authentication. || `embabel.agent.platform.models.ocigenai.profile` | String | `DEFAULT` | OCI config profile name. || `embabel.agent.platform.models.ocigenai.region` | String | *(none)* | OCI region id, such as `us-chicago-1`. Used when an explicit endpoint is not set. || `embabel.agent.platform.models.ocigenai.endpoint` | String | *(none)* | Explicit OCI Generative AI inference endpoint URL. Overrides region-based endpoint selection. || `embabel.agent.platform.models.ocigenai.compartment-id` | String | *(none)* | OCI compartment OCID used for chat and embedding requests. Required. || `embabel.agent.platform.models.ocigenai.serving-mode` | Enum | `ON_DEMAND` | OCI serving mode. Supported values are `ON_DEMAND` and `DEDICATED`. || `embabel.agent.platform.models.ocigenai.endpoint-id` | String | *(none)* | Dedicated serving endpoint OCID. Required when `serving-mode` is `DEDICATED`. || `embabel.agent.platform.models.ocigenai.tenant-id` | String | *(none)* | Tenancy OCID for `SIMPLE`, `SESSION_TOKEN`, or workload identity configuration as required by the selected OCI authentication provider. || `embabel.agent.platform.models.ocigenai.user-id` | String | *(none)* | User OCID for `SIMPLE` or builder-based `SESSION_TOKEN` authentication. || `embabel.agent.platform.models.ocigenai.fingerprint` | String | *(none)* | API key fingerprint for `SIMPLE` or builder-based `SESSION_TOKEN` authentication. || `embabel.agent.platform.models.ocigenai.private-key` | String | *(none)* | PEM private key content for `SIMPLE` authentication. Prefer `private-key-file` where possible. || `embabel.agent.platform.models.ocigenai.private-key-file` | String | *(none)* | Path to a PEM private key file for `SIMPLE` or builder-based `SESSION_TOKEN` authentication. || `embabel.agent.platform.models.ocigenai.pass-phrase` | String | *(none)* | Private key pass phrase, if the configured private key is encrypted. || `embabel.agent.platform.models.ocigenai.session-token` | String | *(none)* | Session token value for builder-based `SESSION_TOKEN` authentication. || `embabel.agent.platform.models.ocigenai.session-token-file` | String | *(none)* | Path to a session token file for builder-based `SESSION_TOKEN` authentication. || `embabel.agent.platform.models.ocigenai.workload-identity-token-path` | String | *(none)* | Workload identity token path for `WORKLOAD_IDENTITY` authentication. || `embabel.agent.platform.models.ocigenai.federation-endpoint` | String | *(none)* | Optional federation endpoint for principal-based authentication. || `embabel.agent.platform.models.ocigenai.max-attempts` | Int | `10` | Maximum retry attempts for OCI GenAI requests. || `embabel.agent.platform.models.ocigenai.backoff-millis` | Long | `5000` | Initial retry backoff in milliseconds. || `embabel.agent.platform.models.ocigenai.backoff-multiplier` | Double | `5.0` | Retry backoff multiplier. || `embabel.agent.platform.models.ocigenai.backoff-max-interval` | Long | `180000` | Maximum retry backoff interval in milliseconds. |
If your application exposes Spring Boot Actuator `env` or `configprops` values, secure those endpoints and sanitize OCI
credential property names such as `pass-phrase`, `session-token` and `private-key`.
##### HTTP Client Configuration
From `NettyClientFactoryProperties` - configuration for the HTTP client used by model providers (OpenAI, Anthropic, etc.) when making API calls.Embabel uses Reactor Netty as the HTTP client for improved performance and non-blocking I/O.
This is particularly important for LLM API calls which can have long response times.
###### Dependency Requirement
To use the Netty client, you must manually add the following autoconfiguration dependency to your project:For Gradle:
| Property | Type | Default | Description || `embabel.agent.platform.http-client.connect-timeout` | Duration | `25s` | Connection timeout for establishing HTTP connections to model providers || `embabel.agent.platform.http-client.read-timeout` | Duration | `1m` | Read timeout (response timeout) for receiving responses from model providers. Increase this value for models that generate long responses or when using extended thinking features. |
Example Configuration
| | For models with extended thinking enabled (like Claude with thinking mode), consider increasing `read-timeout` to `10m` or higher to accommodate longer processing times. |

###### When to Adjust Timeouts

- **Long-running LLM calls**: If you experience timeout errors during complex reasoning tasks, increase `read-timeout`
- **Slow network environments**: Increase `connect-timeout` if connection establishment is failing
- **Streaming responses**: The `read-timeout` applies to the initial response; streaming content has its own handling
| | The HTTP client configuration applies to all model providers that use Spring’s `RestClient` and `WebClient`, including OpenAI, Anthropic, and OpenAI-compatible endpoints. |

##### Server-Sent Events
From `AgentPlatformProperties.SseConfig` - server-sent events configuration.
| Property | Type | Default | Description || `embabel.agent.platform.sse.max-buffer-size` | Int | `100` | Maximum buffer size for SSE || `embabel.agent.platform.sse.max-process-buffers` | Int | `1000` | Maximum number of process buffers |

##### REST Endpoints
From `AgentPlatformProperties.RestConfig` - toggles for the platform’s built-in REST endpoints.
Each flag controls whether the corresponding endpoint is exposed.
When disabled, the corresponding controller bean is not registered, so the endpoint is absent from
Swagger/OpenAPI documentation and routing rejects calls (HTTP 404, or HTTP 405 if another method
remains mapped at the same path).
| Property | Type | Default | Description || `embabel.agent.platform.rest.process-status-enabled` | Boolean | `true` | Whether `GET /api/v1/process/{id}` (process status) is exposed || `embabel.agent.platform.rest.process-kill-enabled` | Boolean | `true` | Whether `DELETE /api/v1/process/{id}` (terminate process) is exposed || `embabel.agent.platform.rest.process-events-enabled` | Boolean | `true` | Whether `GET /events/process/{id}` (SSE event stream) is exposed. When disabled, the SSE controller is not registered. |

##### Test Configuration
From `AgentPlatformProperties.TestConfig` - test configuration.
| Property | Type | Default | Description || `embabel.agent.platform.test.mock-mode` | Boolean | `true` | Whether to enable mock mode for testing |

##### Process Repository Configuration
From `ProcessRepositoryProperties` - configuration for the agent process repository.
| Property | Type | Default | Description || `embabel.agent.platform.process-repository.window-size` | Int | `1000` | Maximum number of agent processes to keep in memory when using default `InMemoryAgentProcessRepository`. When exceeded, oldest processes are evicted. |

##### Standalone LLM Configuration

###### LLM Operations Prompts
From `LlmOperationsPromptsProperties` - properties for ChatClientLlmOperations operations.
| Property | Type | Default | Description || `embabel.llm-operations.prompts.maybe-prompt-template` | String | `maybe_prompt_contribution` | Template to use for the "maybe" prompt, which can enable a failure result if the LLM does not have enough information to create the desired output structure || `embabel.llm-operations.prompts.generate-examples-by-default` | Boolean | `true` | Whether to generate examples by default || `embabel.llm-operations.prompts.default-timeout` | Duration | `60s` | Default timeout for operations |

###### LLM Data Binding
From `LlmDataBindingProperties` - data binding properties with retry configuration for LLM operations.
| Property | Type | Default | Description || `embabel.llm-operations.data-binding.max-attempts` | Int | `10` | Maximum retry attempts for data binding || `embabel.llm-operations.data-binding.fixed-backoff-millis` | Long | `30` | Fixed backoff time in milliseconds between retries |

##### Additional Model Providers

###### AWS Bedrock
From `BedrockProperties` - AWS Bedrock model configuration properties.
| Property | Type | Default | Description || `embabel.models.bedrock.models` | List | `[]` | List of Bedrock models to configure || `embabel.models.bedrock.models[].name` | String | `""` | Model name || `embabel.models.bedrock.models[].knowledge-cutoff` | String | `""` | Knowledge cutoff date || `embabel.models.bedrock.models[].input-price` | Double | `0.0` | Input token price || `embabel.models.bedrock.models[].output-price` | Double | `0.0` | Output token price |

###### ONNX Embeddings
From `OnnxEmbeddingProperties` - configuration for local ONNX embedding models.
| Property | Type | Default | Description || `embabel.agent.platform.models.onnx.embeddings.enabled` | Boolean | `true` | Whether to enable ONNX embedding service || `embabel.agent.platform.models.onnx.embeddings.model-uri` | String | *(HuggingFace all-MiniLM-L6-v2)* | URI to the ONNX model file (HuggingFace URL or `file://` path) || `embabel.agent.platform.models.onnx.embeddings.tokenizer-uri` | String | *(HuggingFace all-MiniLM-L6-v2)* | URI to the tokenizer JSON file || `embabel.agent.platform.models.onnx.embeddings.dimensions` | Int | `384` | Embedding dimensions || `embabel.agent.platform.models.onnx.embeddings.model-name` | String | `all-MiniLM-L6-v2` | Name for the embedding model || `embabel.agent.platform.models.onnx.embeddings.cache-dir` | String | `~/.embabel/models` | Local cache directory for downloaded model files |

###### Docker Local Models
From `DockerProperties` - configuration for Docker local models (OpenAI-compatible).
| Property | Type | Default | Description || `embabel.docker.models.base-url` | String | `localhost:12434/engines` | Base URL for Docker model endpoint || `embabel.docker.models.max-attempts` | Int | `10` | Maximum retry attempts || `embabel.docker.models.backoff-millis` | Long | `2000` | Initial backoff time in milliseconds || `embabel.docker.models.backoff-multiplier` | Double | `5.0` | Backoff multiplier || `embabel.docker.models.backoff-max-interval` | Long | `180000` | Maximum backoff interval in milliseconds |

### 4.24. AWS Bedrock Integration

#### 4.24.1. Add the Dependency
To use AWS Bedrock models, add the Bedrock autoconfiguration starter to your project:MavenGradle (Kotlin)Gradle (Groovy)
#### 4.24.2. AWS Configuration
Configure AWS credentials and region using standard Spring AI Bedrock properties.
See the Spring AI Bedrock documentation for credential configuration options.
#### 4.24.3. Available Models

##### Chat Models (Claude)

| Model Name | Model ID | Region | Knowledge Cutoff || `us_claude_3_5_sonnet` | `us.anthropic.claude-3-5-sonnet-20240620-v1:0` | US | 2024-04-01 || `us_claude_3_5_sonnet_v2` | `us.anthropic.claude-3-5-sonnet-20241022-v2:0` | US | 2024-07-01 || `us_claude_3_5_haiku` | `us.anthropic.claude-3-5-haiku-20241022-v1:0` | US | 2024-07-01 || `us_claude_3_7_sonnet` | `us.anthropic.claude-3-7-sonnet-20250219-v1:0` | US | 2024-10-31 || `us_claude_sonnet_4` | `us.anthropic.claude-sonnet-4-20250514-v1:0` | US | 2025-03-01 || `us_claude_opus_4` | `us.anthropic.claude-opus-4-20250514-v1:0` | US | 2025-03-01 || `eu_claude_3_5_sonnet` | `eu.anthropic.claude-3-5-sonnet-20240620-v1:0` | EU | 2024-04-01 || `eu_claude_3_5_sonnet_v2` | `eu.anthropic.claude-3-5-sonnet-20241022-v2:0` | EU | 2024-07-01 || `eu_claude_3_5_haiku` | `eu.anthropic.claude-3-5-haiku-20241022-v1:0` | EU | 2024-07-01 || `eu_claude_3_7_sonnet` | `eu.anthropic.claude-3-7-sonnet-20250219-v1:0` | EU | 2024-10-31 || `eu_claude_sonnet_4` | `eu.anthropic.claude-sonnet-4-20250514-v1:0` | EU | 2025-03-01 || `eu_claude_opus_4` | `eu.anthropic.claude-opus-4-20250514-v1:0` | EU | 2025-03-01 || `apac_claude_3_5_sonnet` | `apac.anthropic.claude-3-5-sonnet-20240620-v1:0` | APAC | 2024-04-01 || `apac_claude_3_5_sonnet_v2` | `apac.anthropic.claude-3-5-sonnet-20241022-v2:0` | APAC | 2024-07-01 || `apac_claude_3_5_haiku` | `apac.anthropic.claude-3-5-haiku-20241022-v1:0` | APAC | 2024-07-01 || `apac_claude_3_7_sonnet` | `apac.anthropic.claude-3-7-sonnet-20250219-v1:0` | APAC | 2024-10-31 || `apac_claude_sonnet_4` | `apac.anthropic.claude-sonnet-4-20250514-v1:0` | APAC | 2025-03-01 || `apac_claude_opus_4` | `apac.anthropic.claude-opus-4-20250514-v1:0` | APAC | 2025-03-01 |

##### Embedding Models

| Model Name | Model ID | Type || `titan_embed_image_v1` | `amazon.titan-embed-image-v1` | Titan || `titan_embed_text_v1` | `amazon.titan-embed-text-v1` | Titan || `titan_embed_text_v2` | `amazon.titan-embed-text-v2:0` | Titan || `cohere_embed_multilingual_v3` | `cohere.embed-multilingual-v3` | Cohere || `cohere_embed_english_v3` | `cohere.embed-english-v3` | Cohere |

#### 4.24.4. Configuration

##### Retry Configuration

#### 4.24.5. Adding New Models
To add new Bedrock models, edit the configuration file:
```
embabel-agent-autoconfigure/models/embabel-agent-bedrock-autoconfigure/
 src/main/resources/models/bedrock-models.yml
```

##### Adding a Chat Model

##### Adding an Embedding Model
Model type must be either `titan` or `cohere`.
#### 4.24.6. See Also

- Configuration Reference
- Spring AI Bedrock Integration