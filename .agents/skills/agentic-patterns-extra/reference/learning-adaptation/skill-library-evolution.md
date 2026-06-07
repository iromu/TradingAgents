---
title: "Skill Library Evolution"
status: "validated-in-production"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Anthropic Engineering", "Imprint (Will Larson)", "Amp (Nicolay)"]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/skill-library-evolution.md"
tags: [skill-library, evolution, code-reuse, composability, progressive-disclosure]
---

## Problem

Agents frequently solve similar problems across different sessions or workflows. Without a mechanism to preserve and
reuse working code, agents must rediscover solutions each time, wasting tokens and time.

## Solution

Agents persist working code implementations as reusable skills in a `skills/` directory. Over time, these evolve into
well-documented, tested capabilities.

**Skill types:**

- **Atomic skills**: Single-purpose functions (e.g., `analyze_sentiment`)
- **Composite skills**: Multi-step workflows combining atomic skills

**Evolution path:** Ad-hoc code → Save working solution → Reusable function → Documented skill → Agent capability

**Progressive disclosure (Imprint approach):**
Instead of loading all skills into context, inject skill descriptions into system prompt and provide a `load_skills`
tool for full content on demand.

**Lazy-loading MCP tools via skills (Amp approach):**
Bind MCP servers to skills with selective tool loading. Example: chrome-devtools MCP (26 tools = 17k tokens) →
lazy-loaded subset (4 tools = 1.5k tokens, 91% reduction).

## Evidence

- **Evidence Grade:** `validated-in-production`
- **Key Findings:**
    - Anthropic Engineering: Code Execution with MCP (2024)
    - Imprint (Will Larson): Progressive disclosure with on-demand loading
    - Amp (Nicolay): 91% token reduction with lazy-loaded MCP tools

## How to use it

**Implementation phases:**

1. **Ad-hoc → Saved**: Agent writes code, saves working solution to `skills/`
2. **Saved → Reusable**: Refactor for generalization, add error handling
3. **Reusable → Documented**: Add docstrings with purpose, parameters, returns, examples
4. **Documented → Capability**: Agent discovers skills through directory listing

**Skill organization:**

```
skills/
├── README.md                 # Index of available skills
├── data_processing/
│   ├── csv_to_json.py
│   └── filter_outliers.py
└── tests/
    └── test_csv_to_json.py
```

## Trade-offs

**Pros:** Builds agent capability over time, reduces redundant problem-solving, creates organizational knowledge,
enables composition of higher-level capabilities.
**Cons:** Requires discipline to save and organize, skills can become stale, needs maintenance/testing infrastructure.

## References

* Anthropic Engineering: Code Execution with MCP (2024)
* [Building an internal agent: Adding support for Agent Skills](https://lethain.com/agents-skills/) - Will Larson (
  Imprint, 2025)
* [Efficient MCP Tool Loading](https://ampcode.com/news/lazy-load-mcp-with-skills) - Amp (Nicolay, 2025)
*
Related: [Compounding Engineering Pattern](compounding-engineering-pattern.md), [CLI-First Skill Design](../tool-use-environment/cli-first-skill-design.md)
