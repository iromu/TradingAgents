---
title: "Compounding Engineering Pattern"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Dan Shipper (Every)", "Every Engineering Team"]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/compounding-engineering-pattern.md"
tags: [compounding, learning, codification, prompts, slash-commands, knowledge-management]
---

## Problem

Traditional software engineering has **diminishing returns**: each feature added increases complexity, making subsequent
features harder to build. Technical debt accumulates, onboarding takes longer, and new team members struggle.

With AI coding agents, this problem is amplified—agents make the same mistakes repeatedly because learnings aren't
systematically captured and codified.

## Solution

Flip the equation: make each feature **compound** by codifying all learnings into reusable agent instructions. When you
complete a feature, document:

1. **What worked in the plan** and what needed adjustment
2. **Issues discovered during testing** that weren't caught earlier
3. **Common mistakes** the agent made
4. **Patterns and best practices** that should be reused

Then embed these insights into:

- **CLAUDE.md / system prompts**: Global coding standards
- **Slash commands**: Repeatable workflows (e.g., `/test-with-validation`)
- **Subagents**: Specialized validators (e.g., security review agent)
- **Hooks**: Automated checks that prevent regressions

**Result:** Each feature makes the next easier because the codebase becomes increasingly "self-teaching."

## Evidence

- **Evidence Grade:** `emerging`
- **Key Findings:**
    - Dan Shipper (Every): "In normal engineering, every feature you add, it makes it harder to add the next feature. In
      compounding engineering, your goal is to make the next feature easier to build."
    - Every engineering team codifies all learnings back into prompts, subagents, and slash commands
    - Enables non-experts to be productive immediately

## How to use it

**During feature development:**

1. Track what the agent got wrong initially
2. Note which parts of the plan needed revision
3. Document edge cases discovered during testing

**After completion:**

1. Update `CLAUDE.md` with new coding standards
2. Create slash commands for repeated workflows
3. Build subagents for specialized validation
4. Add hooks to prevent common mistakes automatically

## Trade-offs

**Pros:** Accelerating productivity, knowledge preservation, better onboarding, reduced repetition, living
documentation.
**Cons:** Upfront time investment, maintenance overhead, over-specification risk, prompt bloat.

## References

* Dan Shipper (Every): "We codify all the learnings from everything we've done... we codify them back into all the
  prompts and all the subagents and all the slash commands."
* [AI & I Podcast: How to Use Claude Code Like the People Who Built It](https://every.to/podcast/transcript-how-to-use-claude-code-like-the-people-who-built-it)
*
Related: [Memory Synthesis from Execution Logs](../context-memory/memory-synthesis-from-execution-logs.md), [Skill Library Evolution](skill-library-evolution.md)
