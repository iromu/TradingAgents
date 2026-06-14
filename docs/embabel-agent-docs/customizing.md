Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.32. Customizing Embabel

#### 4.32.1. Adding LLMs
You can add custom LLMs as Spring beans by implementing the `LlmService` interface.
Embabel provides `SpringAiLlmService` for wrapping Spring AI `ChatModel` instances.
##### Using `SpringAiLlmService`
`SpringAiLlmService` implements the `LlmService` interface and provides framework-agnostic LLM operations
including support for the Embabel tool loop and message sender abstraction.JavaKotlin
| **1** | The name of the LLM (used for model selection). || **2** | The provider name, such as "OpenAI" or "Anthropic". || **3** | The Spring AI `ChatModel` instance. || **4** | Customize with an `OptionsConverter` implementation to convert Embabel `LlmOptions` to Spring AI `ChatOptions`. || **5** | Set the knowledge cutoff date if available. |

##### LLM Configuration Options
`SpringAiLlmService` supports the following configuration:
- name (required)
- provider, such as "Mistral" (required)
- `OptionsConverter` to convert Embabel `LlmOptions` to Spring AI `ChatOptions`
- **knowledge cutoff date** (if available)
- any additional `PromptContributor` objects to be used in all LLM calls.
If knowledge cutoff date is provided, add the `KnowledgeCutoffDate` prompt contributor.
- pricing model (if available)A common requirement is to add an OpenAI-compatible LLM.
This can be done by extending the `OpenAiCompatibleModelFactory` class as follows:JavaKotlin
#### 4.32.2. Adding embedding models
Embedding models can also be added as beans of the Embabel type `EmbeddingService`.
Use the `SpringAiEmbeddingService` class to wrap a Spring AI `EmbeddingModel`.Typically, this is done in an `@Configuration` class like this:JavaKotlin
#### 4.32.3. Bring Your Own Key (BYOK)
By default, Embabel resolves LLMs through autoconfiguration: you set one or more API keys as an
environment variable or property (e.g. `ANTHROPIC_API_KEY`), and the relevant autoconfigure
module registers a pool of `LlmService` beans at startup.
This is the right approach for a platform-level key shared across all users.BYOK is for cases where the key is not known at startup, or where you want to resolve an
`LlmService` on the fly:
- **User-supplied keys** — each user provides their own API key; the application must validate
it and wire it into the prompt runner for that session.
- **End-to-end testing** — spin up a real `LlmService` with a dedicated test key outside a
full Spring context.
- **Multi-tenant or cost-controlled apps** — select a provider dynamically based on per-tenant
configuration or available quota.Embabel provides factory classes that validate a key and return a ready `LlmService`,
plus a `detectProvider()` utility that concurrently probes multiple providers and returns
the first that accepts the key.
| | `buildValidated()` and `detectProvider()` handle key validation only.
Embabel does not store, log, or otherwise manage the key — the validated `LlmService`
is returned to the caller, who is responsible for secure key handling: transmission
over HTTPS only, no plaintext logging, limiting key lifetime, and revoking cached
services on logout or key rotation. |

##### Building a validated service (known provider)
Use this when the provider is already known — for example, a per-provider field in a settings UI.`buildValidated()` makes a single probe call with no retries.
On success it returns a production `LlmService`; on failure it throws `InvalidApiKeyException`.
##### Auto-detecting the provider
Use this when a user pastes a key without specifying a provider — for example, a sign-up flow
that accepts keys from any supported provider.`detectProvider()` races the candidates concurrently using virtual threads and returns the
first `LlmService` that validates successfully. The detected provider is available as
`service.provider` on the result.A single-argument call is valid — it validates against one provider without concurrency,
which is the right path for a settings flow where the provider is known but you still want
`detectProvider’s consistent error handling.If all candidates throw `InvalidApiKeyException`, `detectProvider` also throws
`InvalidApiKeyException`.
##### Overriding the validation model
Each factory validates the key using a default model (e.g. `gpt-4.1-mini` for OpenAI,
`claude-haiku-4-5` for Anthropic). Override this if the key only grants access to a
specific set of models:
##### Adding support for another provider
Any provider that exposes an OpenAI-compatible HTTP API can be added as a one-liner extension
function on `OpenAiCompatibleModelFactory.Companion`:
| **1** | The cheapest model available on the provider, used for the key-validation probe. || **2** | The provider name; returned as `service.provider` after detection. |
The extension function integrates with `detectProvider` like any built-in factory:
##### Using the validated service
Once you have an `LlmService`, pass it directly to `PromptRunner` or `Ai` via
`withLlmService()`:JavaKotlinInternally this flows through the same model selection path as all other LLM resolution via
`PreResolvedModelSelectionCriteria` — no separate resolution path is needed.
##### Error handling
`InvalidApiKeyException` is in `com.embabel.common.byok` and carries no provider-specific
implementation details.
##### Security considerations
The BYOK factories validate keys and return a ready `LlmService` — key lifecycle management
is entirely the caller’s responsibility.As a reference implementation, Guide holds keys in server-side memory only (`UserKeyStore`).
When a key is validated, the client receives an AES-256-GCM encrypted blob — keyed by a
secret known only to the server — for local-storage caching. A stolen blob is useless without
the server’s decryption key. On page reload the client sends the blob back; the server
decrypts it and restores the in-memory key. Keys are never written to disk or a database.
| | If you need to implement support for a provider not covered by the built-in factories,
see Advanced: Custom LLM Integration. |

#### 4.32.4. Configuration via `application.properties` or `application.yml`
You can specify Spring configuration, your own configuration and Embabel configuration in the regular Spring configuration files.
Profile usage will work as expected.
#### 4.32.5. Customizing logging
You can customize logging as in any Spring application.For example, in `application.properties` you can set properties like:You can also configure logging via a `logback-spring.xml` file if you have more sophisticated requirements.See the Spring Boot Logging reference.By default, many Embabel examples use personality-based logging experiences such as Star Wars.
You can disable this by updating application.properties accordingly.Remove the `embabel.agent.logging.personality` key to disable personality-based logging.As all logging results from listening to events via an `AgenticEventListener`, you can also easily create your own customized logging.