# AGENTS.md — TradingAgents (Gekko)

Guidance for AI agents working on this project.

## The .wiki Is the Primary Knowledge Source

**Read `.wiki/index.md` first.** Everything you need to understand this project is there, organized for progressive disclosure.

The wiki is structured so you can start at a high level and drill down only when needed:

| Section | When to read it |
|---------|----------------|
| `index.md` | **Always** — start here for context |
| `overview/` | Understanding what the project is and how to run it |
| `features/` | Understanding what the system does |
| `flows/` | Understanding step-by-step behavior |
| `concepts/` | Understanding why the system is designed this way |
| `entities/` | Understanding data shapes and models |
| `risks/` | Before making changes — know what not to break |
| `reference/` | Code-shaped details when you need specifics |

### Progressive Disclosure Principle

**Do not dump all knowledge at once.** Use the wiki to guide your understanding:

1. Read the index to get the lay of the land
2. Read only the pages relevant to the task at hand
3. Follow `[[wiki-links]]` to drill deeper when you encounter something unfamiliar
4. Verify against source code when the wiki is ambiguous or you need exact line numbers
5. Update the wiki when you learn something new that isn't documented

This keeps responses concise and relevant. The wiki exists so you don't have to re-explain the project in every conversation.

### After Making Changes

When you modify code, prompts, or behavior:

1. **Update the relevant wiki page(s)** — reflect what changed
2. **Run `ingest wiki`** — updates the wiki index with the new commit SHA so future sessions know what's current

## Project Snapshot

- **What:** Multi-agent trading research platform
- **Stack:** Spring Boot 3.5.13, Embabel 0.5.0-SNAPSHOT, Java 25, Maven
- **LLM:** OpenAI-compatible endpoint (LiteLLM at `http://spark.local:4000`)
- **Data:** Alpha Vantage API, Yahoo Finance, TA4J
- **UI:** Thymeleaf + HTMX

## Key Files

| File | Role |
|------|------|
| `.wiki/index.md` | Wiki index — always start here |
| `src/main/java/com/embabel/gekko/agent/TraderAgent.java` | Main agent orchestrator |
| `src/main/java/com/embabel/gekko/config/TraderAgentConfig.java` | Configuration |
| `src/main/resources/application.yaml` | App config |
| `src/main/resources/prompts/` | Agent prompt templates (Jinja) |
| `src/main/java/com/embabel/gekko/util/FileCache.java` | Disk-based caching layer |
| `src/main/java/com/embabel/gekko/dataflows/AlphaVantageService.java` | Alpha Vantage API client |

## Code Conventions

- **No comments unless necessary** — code should be self-documenting
- **Java records for data** — use records for DTOs, state, and feedback types
- **Embabel annotations** — use `@Agent`, `@Action`, `@AchievesGoal` for agent code
- **Prompt templates in Jinja** — agent behavior lives in `prompts/*.jinja` files
- **Cache via FileCache** — use `FileCache.getOrCompute()` for caching

## Workflow

1. **Understand** — read the relevant wiki page(s), then verify against source
2. **Implement** — make changes to code and prompts
3. **Verify** — run `./mvnw verify` to check the build
4. **Update wiki** — if behavior changed, update the relevant wiki page(s)
5. **Ingest** — run `ingest wiki` to update the wiki index
6. **Commit** — the user handles commits. Never run `git commit`.

## Known Risks (see `.wiki/risks/`)

- **Debate convergence** — fixed iteration count, no convergence check
- **FileCache race condition** — potential for duplicate computation
- **Cache key bugs** — some API methods ignore query parameters in cache keys
- **Hardcoded credentials** — API keys in `application-local.yaml` (gitignored)

## When in Doubt

1. Check the `.wiki/` first — the answer is probably there
2. Check `git log` — recent commits often explain why things are the way they are
3. Ask the user — don't guess when something is genuinely unclear
