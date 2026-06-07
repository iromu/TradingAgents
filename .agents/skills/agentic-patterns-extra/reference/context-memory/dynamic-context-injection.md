---
title: "Dynamic Context Injection"
status: "established"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Boris Cherny (via Claude Code)"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/dynamic-context-injection.md"
tags: [context-management, lazy-loading, slash-commands, at-mention, interactive-context]
---

## Problem

While layered configuration files provide good baseline context, agents often need specific pieces of information (e.g.,
contents of a particular file, output of a script, predefined complex prompt) on-demand during an interactive session.
Constantly editing static context files or pasting large chunks of text into prompts is inefficient.

## Solution

Implement mechanisms for users to dynamically inject context into the agent's working memory during a session. Common
approaches include:

- **File/Folder At-Mentions:** Allowing users to type a special character (e.g., `@`) followed by a file or folder
  path (e.g., `@src/components/Button.tsx` or `@app/tests/`). The agent then ingests the content of the specified file
  or a summary of the folder into its current context for the ongoing task.
- **Custom Slash Commands:** Enabling users to define reusable, named prompts or instructions in separate files (e.g.,
  in `~/.claude/commands/foo.md`). These can be invoked with a slash command (e.g., `/user:deployment`), causing their
  content to be loaded into the agent's context.

## Evidence

- **Evidence Grade:** `established`
- **Universal Adoption:** Implemented across all major AI coding platforms as the de facto standard
- **Security-Critical:** Path traversal and credential exfiltration are primary concerns requiring allowlist validation
  and secret scanning

## How to use it

- Use this when model quality depends on selecting or retaining the right context.
- Start with strict context budgets and explicit memory retention rules.
- Measure relevance and retrieval hit-rate before increasing memory breadth.
- Implement security controls: allowlist-based directory access, regex-based credential scanning, file size limits

## Trade-offs

* **Pros:** Raises answer quality by keeping context relevant and reducing retrieval noise.
* **Cons:** Requires ongoing tuning of memory policies and indexing quality.

## References

- Based on the at-mention and slash command features described in "Mastering Claude Code: Boris Cherny's Guide &
  Cheatsheet," section IV.
- Lewis, P., et al. (2020). "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks." NeurIPS 2020.
- Beurer-Kellner, M., et al. (2025). "Design Patterns for Securing LLM Agents against Prompt Injections." arXiv:
  2506.08837.
