---
title: "LLM Provider Layer"
type: "feature"
status: "active"
language: "default"
source_paths: [tradingagents/llm_clients/]
updated_at: "2026-06-14"
---

# LLM Provider Layer

The LLM client layer (`tradingagents/llm_clients/`) provides a unified abstraction over multiple LLM providers. Each provider gets its own client module, and a factory creates the right client based on configuration.

## Supported providers

| Provider | `llm_provider` value | API key env var |
|---|---|---|
| OpenAI | `openai` | `OPENAI_API_KEY` |
| Anthropic | `anthropic` | `ANTHROPIC_API_KEY` |
| Google (Gemini) | `google` | `GOOGLE_API_KEY` |
| xAI (Grok) | `xai` | `XAI_API_KEY` |
| DeepSeek | `deepseek` | `DEEPSEEK_API_KEY` |
| Qwen (Intl) | `qwen` | `DASHSCOPE_API_KEY` |
| Qwen (China) | `qwen` | `DASHSCOPE_CN_API_KEY` |
| GLM (Intl) | `glm` | `ZHIPU_API_KEY` |
| GLM (China) | `glm` | `ZHIPU_CN_API_KEY` |
| MiniMax (Global) | `minimax` | `MINIMAX_API_KEY` |
| MiniMax (China) | `minimax` | `MINIMAX_CN_API_KEY` |
| OpenRouter | `openrouter` | `OPENROUTER_API_KEY` |
| Azure OpenAI | `azure` | see `.env.enterprise.example` |
| AWS Bedrock | `bedrock` | AWS credentials (env / IAM / ~/.aws/credentials) |
| Ollama (local) | `ollama` | none required |
| OpenAI-compatible | `openai_compatible` | `OPENAI_COMPATIBLE_API_KEY` (optional) |

## Provider-specific settings

Some providers have additional configuration knobs:

| Setting | Provider | Config key | Env var |
|---|---|---|---|
| Thinking level | Google | `google_thinking_level` | — |
| Reasoning effort | OpenAI | `openai_reasoning_effort` | — |
| Effort | Anthropic | `anthropic_effort` | — |
| Backend URL | All | `backend_url` | `TRADINGAGENTS_LLM_BACKEND_URL` |
| Temperature | All | `temperature` | `TRADINGAGENTS_TEMPERATURE` |

## Architecture

```
tradingagents/llm_clients/
├── __init__.py          # Exports: BaseLLMClient, create_llm_client
├── base_client.py       # Abstract BaseLLMClient with normalize_content()
├── factory.py           # create_llm_client() — lazy provider imports
├── openai_client.py     # OpenAI / OpenAI-compatible
├── anthropic_client.py  # Anthropic Claude
├── google_client.py     # Google Gemini
├── azure_client.py      # Azure OpenAI
├── bedrock_client.py    # AWS Bedrock
├── model_catalog.py     # Known model lists per provider
├── capabilities.py      # Provider capability detection
├── validators.py        # Model validation
└── api_key_env.py       # API key environment handling
```

## Key design decisions

- **Lazy imports**: The factory (`factory.py`) lazily imports provider modules so unused providers don't cause import errors
- **Provider kwargs**: Provider-specific settings (thinking level, reasoning effort, effort) are forwarded via `_get_provider_kwargs()` in `TradingAgentsGraph`
- **Temperature**: Cross-provider — forwarded whenever set, but reasoning models largely ignore it
- **No default backend_url**: Each provider falls back to its own default endpoint. Setting a provider-specific URL in config was previously a bug (e.g., OpenAI's `/v1` was forwarded to Gemini)
- **OpenAI-compatible**: Works with any OpenAI-compatible server (vLLM, LM Studio, llama.cpp, custom relay). Set `backend_url` to the server endpoint
