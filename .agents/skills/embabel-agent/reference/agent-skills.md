# Agent Skills

Agent Skills provide a standardized way to extend agent capabilities with reusable, shareable skill packages. Embabel implements the [Agent Skills Specification](https://agentskills.io/specification).

## Overview

An Agent Skill is a directory containing a `SKILL.md` file with YAML frontmatter and markdown instructions. Skills can include bundled resources:

- `scripts/` — Executable scripts (Python, Bash, etc.)
- `references/` — Documentation and reference materials
- `assets/` — Static resources like templates and data files

Skills use **lazy loading: only minimal metadata is included in the system prompt, with full instructions loaded when the skill is activated.

### SKILL.md Format

```yaml
---
name: my-skill
description: A skill that does something useful
license: Apache-2.0
---

# My Skill Instructions

Step-by-step instructions for using this skill...
```

### Skill Directory Structure

```
my-skill/
├── SKILL.md        # Required — metadata and instructions
├── scripts/        # Optional — executable scripts
├── references/     # Optional — documentation
└── assets/         # Optional — static resources
```

## Loading Skills

### From GitHub

The simplest way to load skills is from a GitHub URL:

```java
var skills = new Skills("my-skills", "Skills for my agent")
    .withGitHubUrl("https://github.com/anthropics/skills/tree/main/skills");
```

Supported URL formats:

| Format | Description
|------|------------|
| `https://github.com/owner/repo` | Load from repository root |
| `https://github.com/owner/repo/tree/branch` | Specific branch |
| `https://github.com/owner/repo/tree/branch/path/to/skills` | Specific path |

For more control, use explicit parameters:

```java
var skills = new Skills("my-skills", "Skills for my agent")
    .withGitHubSkills("anthropics", "skills", "skills", "main");
```

### From Local Directories

Load a single skill from a directory containing `SKILL.md`:

```java
var skills = new Skills("my-skills", "Local skills")
    .withLocalSkill("/path/to/my-skill");
```

Load multiple skills from a parent directory:

```java
var skills = new Skills("my-skills", "Local skills")
    .withLocalSkills("/path/to/skills-directory");
```

> **NOTE:** `withLocalSkills` scans immediate subdirectories only (depth 1). It does not recurse into nested directories.

## Using Skills in Actions

The `Skills` class implements `LlmReference`, allowing it to be passed to a `PromptRunner`:

```java
var skills = new Skills("financial-skills", "Financial analysis skills")
    .withGitHubUrl("https://github.com/wshobson/agents/tree/main/plugins/business-analytics/skills");

var response = context.ai()
    .withLlm(llm)
    .withReference(skills)
    .withSystemPrompt("You are a helpful financial analyst.")
    .respond(conversation.getMessages());
```

When skills are added as a reference, the agent can:

- See available skills in the system prompt
- Activate skills to get full instructions
- List and read skill resources

### Combining with Other References

Skills can be combined with other `LlmReference` implementations:

```java
var response = context.ai()
    .withLlm(properties.chatLlm())
    .withReference(
        new LocalDirectory("./data/financial", "Financial data files")
            .withUsageNotes("Search to find files matching user requests.")
    )
    .withReference(
        new Skills("analytics", "Business analytics skills")
            .withGitHubUrl("https://github.com/example/skills/tree/main/analytics")
    )
    .withSystemPrompt("You are a financial analyst assistant.")
    .respond(conversation.getMessages());
```

## Skill Activation

Skills are activated lazily. The system prompt contains only minimal metadata (~50–100 tokens per skill). When an agent needs a skill, it calls the `activate` tool to load full instructions.

The `Skills` class exposes three LLM tools:

| Tool | Description |
|------|-------------|
| `activate(name)` | Load full instructions for a skill |
| `listResources(skillName, resourceType)` | List files in `scripts/`, `references/`, or `assets/` |
| `readResource(skillName, resourceType, fileName)` | Read a resource file |

## Validation

Skills are validated when loaded:

- **Frontmatter validation** — Required fields (`name`, `description`) and field lengths
- **File reference validation** — Paths in instructions (e.g., `scripts/build.sh`) must exist
- **Name matching** — Skill name must match its parent directory name

To disable file reference validation:

```java
var loader = new DefaultDirectorySkillDefinitionLoader(false);
```

## Current Limitations

- **Script execution** — Skills with `scripts/` directories are loaded, but script execution is not yet supported. A warning is logged.
- **`allowed-tools` field** — The `allowed-tools` frontmatter field is parsed but not currently enforced.
---

*Source: Embabel Agent v1.0.0 documentation*
