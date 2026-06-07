---
title: "Iterative Prompt & Skill Refinement"
status: "established"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Will Larson (Imprint)"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/iterative-prompt-skill-refinement.md"
tags: [refinement, iteration, prompts, skills, feedback, multi-mechanism, continuous-improvement]
---

## Problem

Agent usage reveals gaps in prompts, skills, and tools—but how do you systematically improve them? When a workflow fails
or behaves sub-optimally, you need multiple mechanisms to capture feedback and iterate.

## Solution

Implement **multiple complementary refinement mechanisms** that work together:

1. **Responsive Feedback (Primary)** — Monitor internal `#ai` channel for issues, skim workflow interactions daily
2. **Owner-Led Refinement (Secondary)** — Store prompts in editable documents (Notion, Google Docs), include prompt
   links in workflow outputs
3. **Claude-Enhanced Refinement (Specialized)** — Use observability MCP to pull logs into skill repository
4. **Dashboard Tracking (Quantitative)** — Track workflow run frequency, errors, and tool usage

## Evidence

- **Evidence Grade:** `established`
- **Key Findings:**
    - Grounded in RLHF research (human feedback is irreplaceable for alignment)
    - RLAIF demonstrates AI-assisted feedback enables scale
    - Multi-layered approach catches issues different mechanisms miss

## How to use it

**Implementation checklist:**

- [ ] Feedback channel: Internal Slack/Discord for agent issues
- [ ] Editable prompts: Store in Notion/docs, not code
- [ ] Prompt links: Include in every workflow output
- [ ] Log access: Datadog/observability with MCP integration
- [ ] Dashboards: Track workflow runs, errors, tool usage

**Refinement workflow:**

- **Daily:** Skim feedback channel, review workflow interactions
- **Weekly:** Review dashboard metrics for error spikes
- **Ad-hoc:** Pull logs when specific issues reported
- **Quarterly:** Comprehensive prompt/skill audit

## Trade-offs

**Pros:** Multi-layered (catches issues different mechanisms miss), continuous improvement, accessible (anyone can
contribute), data-driven prioritization.
**Cons:** No silver bullet, maintenance overhead for multiple systems, permission complexity, alert fatigue risk.

## References

* [Iterative prompt and skill refinement](https://lethain.com/agents-iterative-refinement/) - Will Larson (Imprint,
  2026)
* [Constitutional AI: Harmlessness from AI Feedback](https://arxiv.org/abs/2212.08073) - Bai et al. (2022)
* [Reflexion: Language Agents with Verbal Reinforcement Learning](https://arxiv.org/abs/2303.11366) - Shinn et al. (
  NeurIPS, 2023)
