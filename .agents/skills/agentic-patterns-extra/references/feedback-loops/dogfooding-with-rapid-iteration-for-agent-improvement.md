---
title: "Dogfooding with Rapid Iteration for Agent Improvement"
status: "validated-in-production"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Cursor", "Anthropic"]
category: "Feedback Loops"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/dogfooding-with-rapid-iteration-for-agent-improvement.md"
tags: [dogfooding, rapid-iteration, feedback-loop, internal-testing, agent-improvement]
---

## Problem

Developing effective AI agents requires understanding real-world usage and quickly identifying areas for improvement. External feedback loops can be slow, and simulated environments may not capture all nuances.

## Solution

The development team extensively uses their own AI agent product ("dogfooding") for their daily software development tasks. This provides:

1. **Direct, Immediate Feedback** — Developers encounter the agent's strengths and weaknesses firsthand
2. **Real-World Problem Solving** — The agent is tested on actual, complex development problems
3. **Internal Experimentation** — Quickly try out new agent features or modifications
4. **Rapid Iteration** — Shortcomings identified can be rapidly addressed before wider release
5. **Honest Assessment** — The team can be brutally honest about a feature's utility

## Evidence

- **Evidence Grade:** `validated-in-production`
- **Key Findings:**
  - Cursor: "That's how we're able to move really quickly and building new features and then throwing away things that clearly don't work"
  - Anthropic: "Internally over 70 or 80 percent of ants use Claude Code every day... we get a post every five minutes"

## How to use it

- Encourage all members of the agent development team to use the agent as their primary tool
- Establish low-friction feedback channels (dedicated Slack/Discord)
- Store prompts and agent instructions in editable documents that anyone can update
- Push experimental features to internal users first for rapid validation
- Be willing to discard what doesn't work

## Trade-offs

* **Pros:** Real-world problem solving, rapid feature validation, quick pivots, reduced risk of shipping unwanted features.
* **Cons:** Requires high internal adoption to be effective, internal users may not represent all customer segments.

## References

- Lukas Möller (Cursor) at 0:04:25: "Cursor is very much driven by kind of solving our own problems"
- Aman Sanger (Cursor) at 0:04:55: "That's how we're able to move really quickly and building new features"
- Cat Wu (Anthropic): "Internally over 70 or 80 percent of ants use Claude Code every day"
- [AI & I Podcast: How to Use Claude Code Like the People Who Built It](https://every.to/podcast/transcript-how-to-use-claude-code-like-the-people-who-built-it)
