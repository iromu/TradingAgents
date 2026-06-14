---
name: embabel-agent-docs-index
description: Master index of all Embabel Agent framework documentation sources
---

# Embabel Agent Documentation Index

This directory contains raw documentation sources for the Embabel Agent framework.

## Sources

- [embabel/embabel-agent](https://github.com/embabel/embabel-agent) — Main framework repository (asciidoc source)
- [embabel/java-agent-template](https://github.com/embabel/java-agent-template) — Java starter template with WriteAndReviewAgent example
- [embabel/embabel-agent-examples](https://github.com/embabel/embabel-agent-examples) — Comprehensive Java examples
- [docs.embabel.com](https://docs.embabel.com) — Published documentation site

## File Listing

### Getting Started
- `getting-started/quickstart.md` — Quickstart guide (from embabel-agent-docs)
- `getting-started/first-agent.md` — Writing Your First Agent guide
- `getting-started/installing.md` — Installation guide
- `getting-started/running.md` — Running Embabel guide
- `getting-started/a-little-ai.md` — Adding AI to your application

### Reference
- `reference/core-concepts.md` — Core concepts (actions, goals, conditions, domain model)
- `reference/annotations.md` — Annotation model (@Agent, @Action, @AchievesGoal, @Condition, @SecureAgentTool, etc.)
- `reference/configuration.md` — Full configuration property reference
- `reference/testing.md` — Unit and integration testing patterns
- `reference/tools.md` — Tools, tool groups, ToolCallContext, subagents
- `reference/domain-objects.md` — Domain objects and @Tool methods
- `reference/planners.md` — GOAP, Utility AI, Hybrid, Supervisor planning
- `reference/states.md` — State patterns, looping, human-in-the-loop
- `reference/invocation.md` — Invocation patterns, Autonomy, REST endpoints
- `reference/guide-server.md` — Guide server, MCP client, WebSocket chat

### Examples
- `examples/write-and-review-agent.md` — WriteAndReviewAgent from java-agent-template
- `examples/star-news-finder.md` — StarNewsFinder from embabel-agent-examples
- `examples/fact-checker.md` — FactChecker with parallel execution
- `examples/injected-component.md` — InjectedComponent with @Component injection

### Templates
- `templates/pom.xml` — Maven pom.xml template
- `templates/application.yml` — Application configuration template
