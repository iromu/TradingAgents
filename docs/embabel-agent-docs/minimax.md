Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.25. MiniMax Integration
MiniMax is a Chinese AI company offering high-performance LLMs via an OpenAI-compatible API.
Embabel integrates MiniMax as a first-class provider using the same `OpenAiCompatibleModelFactory` pattern as other OpenAI-compatible providers.
#### 4.25.1. Add the Dependency
MavenGradle (Kotlin)Gradle (Groovy)
#### 4.25.2. API Key Configuration
Set your MiniMax API key via environment variable (recommended) or Spring property:Or in `application.yml`:
| | The environment variable `MINIMAX_API_KEY` takes precedence over the property value.
Use the property for local development and the environment variable in production deployments. |

#### 4.25.3. Available Models

| Model Name | Model ID | Context Window | Input (per 1M tokens) | Output (per 1M tokens) || `MiniMax-M3` | `MiniMax-M3` | 512K tokens | $0.60 | $2.40 || `MiniMax-M2.7` | `MiniMax-M2.7` | 192K tokens | $1.10 | $4.40 || `MiniMax-M2.7-highspeed` | `MiniMax-M2.7-highspeed` | 192K tokens | $0.55 | $2.20 |
`MiniMax-M3` is the latest flagship model — the best default choice and significantly cheaper than `MiniMax-M2.7`.
`MiniMax-M2.7` is the previous flagship, retained for backward compatibility and projects that need its 192K context window.
`MiniMax-M2.7-highspeed` trades some quality for significantly lower latency and cost — a good choice for intermediate steps in a multi-action agent flow.
| | Embabel’s per-step LLM selection makes MiniMax models particularly well-suited to mixed strategies: use `MiniMax-M2.7-highspeed` for extraction and classification steps, and reserve `MiniMax-M3` (or a premium model) only for the final reasoning step. |

#### 4.25.4. Using MiniMax Models
Reference models by name in `@LlmCall` or programmatically via `ai.withLlm()`:KotlinJavaOr map MiniMax models to roles in your configuration:Then reference by role with the `#` prefix:KotlinJava
#### 4.25.5. Temperature Clamping
MiniMax models require temperature in the range `(0.0, 1.0]` — a value of exactly `0.0` is not permitted.
Embabel’s `MiniMaxOptionsConverter` clamps temperature automatically:
- Values `⇐ 0.0` are raised to `0.01`
- Values `> 1.0` are lowered to `1.0`A `DEBUG` log message is emitted whenever clamping occurs.
No action is required on your part — this is handled transparently.
#### 4.25.6. Configuration Reference

#### 4.25.7. See Also

- Working with LLMs
- Configuration Reference
- MiniMax AI